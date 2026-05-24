-- V17 — Grille de sanctions configurable selon règlement intérieur
-- Étend les types de sanctions (retards par tranche, échec tontine, troubles)
-- et ajoute les montants paramétrables par tontine.

-- 1. Remplacer le CHECK constraint sur le type de sanction
ALTER TABLE sanctions DROP CONSTRAINT IF EXISTS sanctions_type_sanction_check;
ALTER TABLE sanctions ADD CONSTRAINT sanctions_type_sanction_check
    CHECK (type_sanction IN (
        'RETARD_COTISATION',
        'ABSENCE_REUNION',
        'RETARD_REUNION_T1',
        'RETARD_REUNION_T2',
        'RETARD_REUNION_T3',
        'ECHEC_TONTINE_AVANT',
        'ECHEC_TONTINE_APRES',
        'TROUBLE_BAGARRE',
        'TROUBLE_ENGUEULADE',
        'TROUBLE_INSULTE',
        'AUTRE'
    ));

-- 2. Grille de montants configurables par tontine
ALTER TABLE tontines
    ADD COLUMN IF NOT EXISTS montant_retard_reunion_t1        NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_retard_reunion_t2        NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_retard_reunion_t3        NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_echec_tontine_avant      NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_echec_tontine_apres      NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_reverse_beneficiaire     NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_trouble_bagarre          NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_trouble_engueulade       NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS montant_trouble_insulte          NUMERIC(15,2) NOT NULL DEFAULT 0;

COMMENT ON COLUMN tontines.montant_retard_reunion_t1     IS 'Sanction retard tranche 1 (ex: 15h00–15h30)';
COMMENT ON COLUMN tontines.montant_retard_reunion_t2     IS 'Sanction retard tranche 2 (ex: 15h31–16h00)';
COMMENT ON COLUMN tontines.montant_retard_reunion_t3     IS 'Sanction retard tranche 3 (ex: après 16h00)';
COMMENT ON COLUMN tontines.montant_echec_tontine_avant   IS 'Sanction échec tontine — membre n''ayant pas encore bénéficié';
COMMENT ON COLUMN tontines.montant_echec_tontine_apres   IS 'Sanction échec tontine — membre ayant déjà bénéficié';
COMMENT ON COLUMN tontines.montant_reverse_beneficiaire  IS 'Part de la sanction échec_apres reversée au bénéficiaire du tour';
COMMENT ON COLUMN tontines.montant_trouble_bagarre       IS 'Sanction trouble — bagarre';
COMMENT ON COLUMN tontines.montant_trouble_engueulade    IS 'Sanction trouble — engueulade';
COMMENT ON COLUMN tontines.montant_trouble_insulte       IS 'Sanction trouble — insulte';