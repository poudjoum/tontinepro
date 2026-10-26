-- =====================================================================
-- V33 – Tontine à lot : abandon d'un membre en cours de session
-- Solde = cotisé − reçu. Positif : avoir gelé, restitué en fin de session.
-- Négatif : dette envers le groupe, remboursée au fil de l'eau.
-- =====================================================================
ALTER TABLE participation_lot
    ADD COLUMN abandonnee BOOLEAN NOT NULL DEFAULT FALSE;

-- Part annulée par un abandon : le co-bénéficiaire concerné ne la touchera pas
ALTER TABLE part_lot
    ADD COLUMN annulee BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE abandon_lot (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id         UUID          NOT NULL REFERENCES sessions_tontine(id) ON DELETE CASCADE,
    membre_id          UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    date_abandon       DATE          NOT NULL,
    motif              VARCHAR(500)  NULL,
    mois_cotises       INTEGER       NOT NULL,
    total_cotise       NUMERIC(15,2) NOT NULL,
    total_recu         NUMERIC(15,2) NOT NULL,
    solde              NUMERIC(15,2) NOT NULL,
    statut             VARCHAR(20)   NOT NULL
                       CHECK (statut IN ('AVOIR_GELE', 'AVOIR_RESTITUE', 'DETTE', 'DETTE_SOLDEE', 'SOLDE_NUL')),
    date_restitution   DATE          NULL,
    montant_rembourse  NUMERIC(15,2) NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_abandon_lot UNIQUE (session_id, membre_id)
);
CREATE INDEX idx_abandon_lot_session ON abandon_lot(session_id);
