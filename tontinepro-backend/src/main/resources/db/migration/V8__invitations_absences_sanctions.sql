-- V8 — Invitations, absences, sanctions + configuration étendue des tontines

-- Colonnes de configuration supplémentaires pour les tontines
ALTER TABLE tontines
    ADD COLUMN IF NOT EXISTS periode_cotisation VARCHAR(20) NOT NULL DEFAULT 'MENSUEL'
        CHECK (periode_cotisation IN ('HEBDOMADAIRE', 'MENSUEL', 'BIMENSUEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL')),
    ADD COLUMN IF NOT EXISTS montant_amende NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_penalite_retard NUMERIC(15,2) NOT NULL DEFAULT 0;

-- Tokens d'invitation à la tontine
CREATE TABLE invitation_tokens (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id     UUID         NOT NULL REFERENCES tontines(id),
    token          VARCHAR(100) NOT NULL UNIQUE,
    utilise        BOOLEAN      NOT NULL DEFAULT false,
    expires_at     TIMESTAMPTZ  NOT NULL,
    cree_par       UUID         NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_invitation_tokens_token   ON invitation_tokens(token);
CREATE INDEX idx_invitation_tokens_tontine ON invitation_tokens(tontine_id);

-- Absences aux réunions
CREATE TABLE absences (
    id               UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id        UUID    NOT NULL REFERENCES membres(id),
    tontine_id       UUID    NOT NULL REFERENCES tontines(id),
    date_reunion     DATE    NOT NULL,
    justifiee        BOOLEAN NOT NULL DEFAULT false,
    motif            TEXT,
    enregistree_par  UUID    NOT NULL REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_absences_membre  ON absences(membre_id);
CREATE INDEX idx_absences_tontine ON absences(tontine_id);
CREATE UNIQUE INDEX idx_absences_unique ON absences(membre_id, date_reunion);

-- Sanctions financières
CREATE TABLE sanctions (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id      UUID         NOT NULL REFERENCES membres(id),
    tontine_id     UUID         NOT NULL REFERENCES tontines(id),
    type_sanction  VARCHAR(30)  NOT NULL
        CHECK (type_sanction IN ('RETARD_COTISATION', 'ABSENCE_REUNION', 'AUTRE')),
    montant        NUMERIC(15,2) NOT NULL,
    motif          TEXT,
    payee          BOOLEAN      NOT NULL DEFAULT false,
    reference_id   UUID,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_sanctions_membre  ON sanctions(membre_id);
CREATE INDEX idx_sanctions_tontine ON sanctions(tontine_id);
