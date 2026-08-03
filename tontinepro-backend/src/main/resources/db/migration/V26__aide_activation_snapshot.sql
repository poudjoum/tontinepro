-- V26 – Snapshot d'activation d'une aide issue du barème
-- =====================================================================
-- À la validation par le bureau, on fige le calcul (mode, montant de référence,
-- nombre de membres, part par membre) et l'option de préfinancement, pour que
-- les chiffres restent cohérents même si la composition change ensuite.

ALTER TABLE aides
    ADD COLUMN mode_calcul       VARCHAR(20)
        CHECK (mode_calcul IS NULL OR mode_calcul IN ('PAR_PERSONNE','FORFAITAIRE')),
    ADD COLUMN montant_reference NUMERIC(15,2),
    ADD COLUMN nb_membres_base   INTEGER,
    ADD COLUMN part_par_membre   NUMERIC(15,2),
    ADD COLUMN prefinance        BOOLEAN NOT NULL DEFAULT FALSE;
