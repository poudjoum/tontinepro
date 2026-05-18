-- =====================================================================
-- V1 – Schéma initial TontinePro
-- PostgreSQL 16 – gen_random_uuid() natif (extension pgcrypto incluse)
-- Convention Flyway : V{n}__{description}.sql
-- =====================================================================

-- ---------------------------------------------------------------------
-- UTILISATEURS & AUTHENTIFICATION
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    telephone       VARCHAR(20)  UNIQUE,
    hashed_password VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'MEMBRE'
                    CHECK (role IN ('ADMIN', 'MEMBRE', 'INVITE')),
    two_fa_enabled  BOOLEAN      NOT NULL DEFAULT FALSE,
    two_fa_secret   VARCHAR(255),
    actif           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_telephone ON users(telephone);

CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ---------------------------------------------------------------------
-- TONTINES (configuration)
-- ---------------------------------------------------------------------
CREATE TABLE tontines (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    nom                   VARCHAR(255) NOT NULL,
    description           TEXT,
    montant_cotisation    NUMERIC(15,2) NOT NULL CHECK (montant_cotisation > 0),
    jour_cotisation       SMALLINT     NOT NULL DEFAULT 1
                          CHECK (jour_cotisation BETWEEN 1 AND 28),
    taux_interet_pret     NUMERIC(5,2) NOT NULL DEFAULT 0
                          CHECK (taux_interet_pret >= 0),
    taux_interet_epargne  NUMERIC(5,2) NOT NULL DEFAULT 0
                          CHECK (taux_interet_epargne >= 0),
    actif                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------
-- MEMBRES (profil métier lié à un user)
-- ---------------------------------------------------------------------
CREATE TABLE membres (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    tontine_id      UUID         NOT NULL REFERENCES tontines(id),
    matricule       VARCHAR(50)  NOT NULL UNIQUE,
    nom             VARCHAR(150) NOT NULL,
    prenom          VARCHAR(150) NOT NULL,
    date_adhesion   DATE         NOT NULL DEFAULT CURRENT_DATE,
    statut          VARCHAR(30)  NOT NULL DEFAULT 'ACTIF'
                    CHECK (statut IN ('ACTIF', 'SUSPENDU', 'RETIRE')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_membres_tontine ON membres(tontine_id);
CREATE INDEX idx_membres_statut ON membres(statut);

CREATE TABLE ayants_droit (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id    UUID         NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    nom          VARCHAR(150) NOT NULL,
    prenom       VARCHAR(150) NOT NULL,
    lien_parente VARCHAR(50)  NOT NULL,
    telephone    VARCHAR(20),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ayants_droit_membre ON ayants_droit(membre_id);

-- ---------------------------------------------------------------------
-- COTISATIONS
-- ---------------------------------------------------------------------
CREATE TABLE cotisations (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id     UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    tontine_id    UUID          NOT NULL REFERENCES tontines(id),
    mois          SMALLINT      NOT NULL CHECK (mois BETWEEN 1 AND 12),
    annee         SMALLINT      NOT NULL CHECK (annee >= 2020),
    montant       NUMERIC(15,2) NOT NULL CHECK (montant >= 0),
    statut        VARCHAR(30)   NOT NULL DEFAULT 'EN_ATTENTE'
                  CHECK (statut IN ('EN_ATTENTE', 'PAYEE', 'EN_RETARD')),
    date_paiement TIMESTAMPTZ,
    reference_paiement VARCHAR(100),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cotisation_periode UNIQUE (membre_id, mois, annee)
);
CREATE INDEX idx_cotisations_membre ON cotisations(membre_id);
CREATE INDEX idx_cotisations_periode ON cotisations(annee, mois);
CREATE INDEX idx_cotisations_statut ON cotisations(statut);

-- ---------------------------------------------------------------------
-- AIDES MUTUELLES
-- ---------------------------------------------------------------------
CREATE TABLE aides (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id       UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    type_aide       VARCHAR(50)   NOT NULL,
    montant_demande NUMERIC(15,2) NOT NULL CHECK (montant_demande > 0),
    montant_accorde NUMERIC(15,2) CHECK (montant_accorde >= 0),
    motif           TEXT          NOT NULL,
    statut          VARCHAR(30)   NOT NULL DEFAULT 'SOUMISE'
                    CHECK (statut IN ('SOUMISE', 'VALIDEE', 'REJETEE', 'PAYEE')),
    justificatif_url VARCHAR(500),
    valide_par      UUID          REFERENCES users(id),
    date_validation TIMESTAMPTZ,
    motif_rejet     TEXT,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_aides_membre ON aides(membre_id);
CREATE INDEX idx_aides_statut ON aides(statut);

-- ---------------------------------------------------------------------
-- ÉPARGNE
-- ---------------------------------------------------------------------
CREATE TABLE comptes_epargne (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id   UUID          NOT NULL UNIQUE REFERENCES membres(id) ON DELETE CASCADE,
    solde       NUMERIC(15,2) NOT NULL DEFAULT 0 CHECK (solde >= 0),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE mouvements_epargne (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    compte_id      UUID          NOT NULL REFERENCES comptes_epargne(id) ON DELETE CASCADE,
    type_mouvement VARCHAR(30)   NOT NULL
                   CHECK (type_mouvement IN ('DEPOT', 'RETRAIT', 'INTERET')),
    montant        NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    solde_apres    NUMERIC(15,2) NOT NULL,
    reference      VARCHAR(100),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_mouvements_compte ON mouvements_epargne(compte_id);
CREATE INDEX idx_mouvements_date ON mouvements_epargne(created_at);

-- ---------------------------------------------------------------------
-- PRÊTS
-- ---------------------------------------------------------------------
CREATE TABLE prets (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id         UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    montant_principal NUMERIC(15,2) NOT NULL CHECK (montant_principal > 0),
    taux_interet      NUMERIC(5,2)  NOT NULL CHECK (taux_interet >= 0),
    duree_mois        SMALLINT      NOT NULL CHECK (duree_mois > 0),
    montant_total     NUMERIC(15,2) NOT NULL,
    statut            VARCHAR(30)   NOT NULL DEFAULT 'DEMANDE'
                      CHECK (statut IN ('DEMANDE', 'VALIDE', 'REJETE',
                                        'EN_COURS', 'SOLDE')),
    valide_par        UUID          REFERENCES users(id),
    date_validation   TIMESTAMPTZ,
    date_decaissement TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_prets_membre ON prets(membre_id);
CREATE INDEX idx_prets_statut ON prets(statut);

CREATE TABLE echeances_pret (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    pret_id         UUID          NOT NULL REFERENCES prets(id) ON DELETE CASCADE,
    numero_echeance SMALLINT      NOT NULL CHECK (numero_echeance > 0),
    date_echeance   DATE          NOT NULL,
    montant_capital NUMERIC(15,2) NOT NULL CHECK (montant_capital >= 0),
    montant_interet NUMERIC(15,2) NOT NULL CHECK (montant_interet >= 0),
    montant_total   NUMERIC(15,2) NOT NULL,
    statut          VARCHAR(30)   NOT NULL DEFAULT 'A_PAYER'
                    CHECK (statut IN ('A_PAYER', 'PAYEE', 'EN_RETARD')),
    date_paiement   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_echeance_pret UNIQUE (pret_id, numero_echeance)
);
CREATE INDEX idx_echeances_pret ON echeances_pret(pret_id);
CREATE INDEX idx_echeances_statut ON echeances_pret(statut);
