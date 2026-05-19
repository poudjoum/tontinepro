-- ============================================================
-- V10 : Sessions de tontine + périodicité avancée
-- ============================================================

-- Renommages colonnes tontines
ALTER TABLE tontines RENAME COLUMN montant_cotisation TO montant_cotisation_min;
ALTER TABLE tontines RENAME COLUMN jour_cotisation TO jour_reference;

-- Changer le type de jour_reference de SMALLINT en INTEGER (nullable)
ALTER TABLE tontines ALTER COLUMN jour_reference DROP NOT NULL;
ALTER TABLE tontines ALTER COLUMN jour_reference TYPE INTEGER;

-- Nouveaux champs tontine
ALTER TABLE tontines
  ADD COLUMN IF NOT EXISTS montant_cotisation_max NUMERIC(15,2),
  ADD COLUMN IF NOT EXISTS montant_consensuel NUMERIC(15,2),
  ADD COLUMN IF NOT EXISTS type_regle_periodicite VARCHAR(40) NOT NULL DEFAULT 'MENSUEL_JOUR_FIXE'
    CHECK (type_regle_periodicite IN ('HEBDOMADAIRE','MENSUEL_JOUR_FIXE','MENSUEL_DERNIER_SAMEDI',
      'MENSUEL_DERNIER_DIMANCHE','MENSUEL_PREMIER_DIMANCHE_APRES','MENSUEL_DIMANCHE_SUR_OU_APRES',
      'TRIMESTRIEL_JOUR_FIXE','SEMESTRIEL_JOUR_FIXE','ANNUEL_JOUR_FIXE','DATE_MANUELLE')),
  ADD COLUMN IF NOT EXISTS date_prochaine_tontine DATE;

-- Migrer periode_cotisation existant vers type_regle_periodicite
UPDATE tontines SET type_regle_periodicite = CASE periode_cotisation
  WHEN 'HEBDOMADAIRE'  THEN 'HEBDOMADAIRE'
  WHEN 'MENSUEL'       THEN 'MENSUEL_JOUR_FIXE'
  WHEN 'BIMENSUEL'     THEN 'MENSUEL_JOUR_FIXE'
  WHEN 'TRIMESTRIEL'   THEN 'TRIMESTRIEL_JOUR_FIXE'
  WHEN 'SEMESTRIEL'    THEN 'SEMESTRIEL_JOUR_FIXE'
  WHEN 'ANNUEL'        THEN 'ANNUEL_JOUR_FIXE'
  ELSE 'MENSUEL_JOUR_FIXE'
END;

-- Table sessions
CREATE TABLE sessions_tontine (
  id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  tontine_id             UUID          NOT NULL REFERENCES tontines(id),
  numero                 INTEGER       NOT NULL,
  date_debut             DATE          NOT NULL,
  date_fin               DATE,
  date_prochaine_tontine DATE,
  statut                 VARCHAR(20)   NOT NULL DEFAULT 'EN_COURS'
    CHECK (statut IN ('EN_COURS','TERMINEE','ANNULEE')),
  nombre_membres         INTEGER       NOT NULL,
  pot_reserve            NUMERIC(15,2) NOT NULL DEFAULT 0,
  created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
  UNIQUE (tontine_id, numero)
);
CREATE INDEX idx_sessions_tontine ON sessions_tontine(tontine_id);

-- Table ordre bénéficiaires
CREATE TABLE ordre_beneficiaires (
  id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id    UUID          NOT NULL REFERENCES sessions_tontine(id),
  membre_id     UUID          NOT NULL REFERENCES membres(id),
  ordre         INTEGER       NOT NULL,
  date_benefice DATE,
  montant_recu  NUMERIC(15,2),
  beneficie     BOOLEAN       NOT NULL DEFAULT false,
  created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  UNIQUE (session_id, ordre),
  UNIQUE (session_id, membre_id)
);
CREATE INDEX idx_ordre_session ON ordre_beneficiaires(session_id);
CREATE INDEX idx_ordre_membre  ON ordre_beneficiaires(membre_id);
