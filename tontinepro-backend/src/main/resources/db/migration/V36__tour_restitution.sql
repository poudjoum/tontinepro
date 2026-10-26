-- =====================================================================
-- V36 - Tour de remboursement (tontine a lot) : dernier tour, finance
-- par les membres choisis par le bureau, qui sert a restituer les avoirs
-- geles des membres partis. L'ecart entre collecte et avoirs est porte
-- par la tresorerie (manque avance, surplus garde).
-- =====================================================================
CREATE TABLE tour_restitution (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id        UUID          NOT NULL REFERENCES sessions_tontine(id) ON DELETE CASCADE,
    date_tour         DATE          NOT NULL,
    clos              BOOLEAN       NOT NULL DEFAULT FALSE,
    date_cloture      DATE          NULL,
    montant_collecte  NUMERIC(15,2) NULL,
    montant_restitue  NUMERIC(15,2) NULL,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tour_restitution_session UNIQUE (session_id)
);

CREATE TABLE tour_restitution_cotisant (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tour_id        UUID          NOT NULL REFERENCES tour_restitution(id) ON DELETE CASCADE,
    membre_id      UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    montant        NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    paye           BOOLEAN       NOT NULL DEFAULT FALSE,
    date_paiement  DATE          NULL,
    CONSTRAINT uq_tour_restitution_cotisant UNIQUE (tour_id, membre_id)
);
CREATE INDEX idx_tour_restitution_cotisant_tour ON tour_restitution_cotisant(tour_id);

-- Avoir rendu par le tour de remboursement (et non individuellement) : rouvrir le tour le regele
ALTER TABLE abandon_lot
    ADD COLUMN restitue_par_tour BOOLEAN NOT NULL DEFAULT FALSE;
