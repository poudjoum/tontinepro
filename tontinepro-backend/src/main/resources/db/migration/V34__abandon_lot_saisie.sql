-- =====================================================================
-- V34 - Depart d'un membre dans une session a lot pas encore figee
-- Le bureau saisit les montants ; les tours non payes du membre sont
-- retires de l'ordre et gardes ici ("ordre:date;...") pour pouvoir
-- annuler le depart.
-- =====================================================================
ALTER TABLE abandon_lot
    ADD COLUMN saisie_manuelle BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN tours_retires   VARCHAR(500) NULL;
