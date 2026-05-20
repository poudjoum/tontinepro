-- V12 — Tontines publiques, documents règlement/statuts, demandes d'adhésion

-- Nouvelles colonnes sur les tontines
ALTER TABLE tontines
  ADD COLUMN IF NOT EXISTS type_acces     VARCHAR(20)  NOT NULL DEFAULT 'OUVERTE'
    CHECK (type_acces IN ('OUVERTE','RESTREINTE')),
  ADD COLUMN IF NOT EXISTS visible        BOOLEAN      NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS description_acces TEXT;

-- Documents officiels de la tontine (règlement intérieur, statuts…)
CREATE TABLE documents_tontine (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  tontine_id      UUID         NOT NULL REFERENCES tontines(id),
  type_document   VARCHAR(30)  NOT NULL
    CHECK (type_document IN ('REGLEMENT_INTERIEUR','STATUTS','AUTRE')),
  nom_fichier     VARCHAR(255) NOT NULL,
  chemin_stockage VARCHAR(500) NOT NULL,
  taille_octets   BIGINT       NOT NULL,
  content_type    VARCHAR(100) NOT NULL,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_docs_tontine ON documents_tontine(tontine_id);

-- Demandes d'adhésion soumises publiquement
CREATE TABLE demandes_adhesion (
  id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  tontine_id   UUID         NOT NULL REFERENCES tontines(id),
  nom          VARCHAR(150) NOT NULL,
  prenom       VARCHAR(150) NOT NULL,
  email        VARCHAR(255) NOT NULL,
  telephone    VARCHAR(20),
  motivation   TEXT,
  statut       VARCHAR(20)  NOT NULL DEFAULT 'EN_ATTENTE'
    CHECK (statut IN ('EN_ATTENTE','APPROUVEE','REJETEE')),
  motif_rejet  TEXT,
  traite_par   UUID REFERENCES users(id),
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_demandes_tontine ON demandes_adhesion(tontine_id);
CREATE INDEX idx_demandes_statut  ON demandes_adhesion(statut);
