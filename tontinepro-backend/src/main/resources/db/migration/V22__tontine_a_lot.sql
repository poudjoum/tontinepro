-- =====================================================================
-- V22 – Mode « tontine à lot » : cagnotte, lots partageables, figeage
-- =====================================================================

-- ── Configuration de la tontine ──────────────────────────────────────
ALTER TABLE tontines
    ADD COLUMN mode                   VARCHAR(20)   NOT NULL DEFAULT 'CLASSIQUE'
                                       CHECK (mode IN ('CLASSIQUE', 'A_LOT')),
    ADD COLUMN montant_lot            NUMERIC(15,2) NULL,
    ADD COLUMN mois_cloture_adhesions INTEGER       NOT NULL DEFAULT 3;

-- ── Session : cagnotte, trésorerie dédiée, figeage ───────────────────
ALTER TABLE sessions_tontine
    ADD COLUMN cagnotte        NUMERIC(15,2) NULL,
    ADD COLUMN tresorerie_lots NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN figee           BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN date_figeage    DATE          NULL;

-- ── Inscriptions « à lot » (mise mensuelle par membre, avant figeage) ─
CREATE TABLE participation_lot (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID          NOT NULL REFERENCES sessions_tontine(id) ON DELETE CASCADE,
    membre_id       UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    montant_mensuel NUMERIC(15,2) NOT NULL,
    date_adhesion   DATE          NOT NULL DEFAULT CURRENT_DATE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_participation_lot UNIQUE (session_id, membre_id)
);
CREATE INDEX idx_participation_lot_session ON participation_lot(session_id);

-- ── Parts d'un lot/tour (lien slot ↔ membres regroupés) ──────────────
CREATE TABLE part_lot (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    ordre_beneficiaire_id  UUID          NOT NULL REFERENCES ordre_beneficiaires(id) ON DELETE CASCADE,
    membre_id              UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    montant_mensuel        NUMERIC(15,2) NOT NULL,
    part_cagnotte          NUMERIC(15,2) NULL,
    CONSTRAINT uq_part_lot UNIQUE (ordre_beneficiaire_id, membre_id)
);
CREATE INDEX idx_part_lot_ordre ON part_lot(ordre_beneficiaire_id);
CREATE INDEX idx_part_lot_membre ON part_lot(membre_id);
