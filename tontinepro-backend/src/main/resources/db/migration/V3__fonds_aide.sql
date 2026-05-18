-- V3 – Fonds d'aide : configuration tontine + tables dédiées
-- =====================================================================

-- Configuration du fonds d'aide sur la tontine
ALTER TABLE tontines
    ADD COLUMN mode_contribution_aide VARCHAR(30) NOT NULL DEFAULT 'AUCUN'
        CHECK (mode_contribution_aide IN ('AUCUN', 'MENSUEL', 'A_LA_BENEFICIATION')),
    ADD COLUMN montant_cotisation_aide NUMERIC(15,2)
        CHECK (montant_cotisation_aide IS NULL OR montant_cotisation_aide > 0);

-- Fonds d'aide (1 par tontine)
CREATE TABLE fonds_aide (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id  UUID          NOT NULL UNIQUE REFERENCES tontines(id) ON DELETE CASCADE,
    solde       NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Historique des mouvements du fonds (crédits et débits)
CREATE TABLE mouvements_fonds_aide (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    fonds_aide_id   UUID          NOT NULL REFERENCES fonds_aide(id) ON DELETE CASCADE,
    type_mouvement  VARCHAR(30)   NOT NULL
                    CHECK (type_mouvement IN ('CONTRIBUTION', 'DECAISSEMENT')),
    montant         NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    solde_apres     NUMERIC(15,2) NOT NULL,
    aide_id         UUID          REFERENCES aides(id),
    membre_id       UUID          REFERENCES membres(id),
    description     VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_mvt_fonds_aide ON mouvements_fonds_aide(fonds_aide_id);
CREATE INDEX idx_mvt_fonds_aide_date ON mouvements_fonds_aide(created_at DESC);

-- Contributions individuelles des membres au fonds d'aide
-- Mode MENSUEL  : une ligne par membre par période (mois/annee)
-- Mode A_LA_BENEFICIATION : une ligne par membre par aide accordée (aide_id renseigné)
CREATE TABLE contributions_fonds_aide (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    fonds_aide_id   UUID          NOT NULL REFERENCES fonds_aide(id) ON DELETE CASCADE,
    membre_id       UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    montant         NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    mois            SMALLINT      CHECK (mois BETWEEN 1 AND 12),
    annee           SMALLINT,
    aide_id         UUID          REFERENCES aides(id),
    statut          VARCHAR(30)   NOT NULL DEFAULT 'A_PAYER'
                    CHECK (statut IN ('A_PAYER', 'PAYEE')),
    date_paiement   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_contrib_fonds_membre ON contributions_fonds_aide(membre_id);
CREATE INDEX idx_contrib_fonds_statut ON contributions_fonds_aide(statut);
CREATE INDEX idx_contrib_fonds_aide_id ON contributions_fonds_aide(aide_id);

-- Initialiser un fonds_aide pour les tontines déjà existantes
INSERT INTO fonds_aide (tontine_id)
SELECT id FROM tontines;
