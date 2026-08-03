-- V24 – Barème des aides : rubriques configurables par tontine (règlement intérieur)
-- =====================================================================
-- Chaque rubrique encode une aide du règlement : un montant de référence
-- interprété selon le mode de calcul.
--   PAR_PERSONNE : montant_reference = part par membre  → total = part × N
--   FORFAITAIRE  : montant_reference = enveloppe totale  → part = total ÷ N
-- (N = nombre de membres actifs, bénéficiaire inclus)

CREATE TABLE rubriques_aide (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id        UUID          NOT NULL REFERENCES tontines(id) ON DELETE CASCADE,
    libelle           VARCHAR(120)  NOT NULL,
    type_aide         VARCHAR(50)   NOT NULL DEFAULT 'AUTRE'
                      CHECK (type_aide IN ('DECES','MALADIE','ACCIDENT','MARIAGE',
                                           'NAISSANCE','SCOLARITE','CALAMITE','AUTRE')),
    mode_calcul       VARCHAR(20)   NOT NULL DEFAULT 'PAR_PERSONNE'
                      CHECK (mode_calcul IN ('PAR_PERSONNE','FORFAITAIRE')),
    montant_reference NUMERIC(15,2) NOT NULL CHECK (montant_reference > 0),
    prefinancable     BOOLEAN       NOT NULL DEFAULT TRUE,
    actif             BOOLEAN       NOT NULL DEFAULT TRUE,
    description       TEXT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rubriques_aide_tontine ON rubriques_aide(tontine_id);
