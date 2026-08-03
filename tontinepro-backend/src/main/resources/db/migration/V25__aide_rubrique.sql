-- V25 – Lien entre une demande d'aide et la rubrique du barème choisie
-- =====================================================================
-- Une demande d'aide peut désormais être issue d'une rubrique du barème
-- (règlement intérieur). Le lien reste nullable pour les aides libres existantes.

ALTER TABLE aides
    ADD COLUMN rubrique_id UUID REFERENCES rubriques_aide(id) ON DELETE SET NULL;

CREATE INDEX idx_aides_rubrique ON aides(rubrique_id);
