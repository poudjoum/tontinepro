-- V30 – Fond de caisse versé avant l'arrivée de la tontine dans l'application
-- =====================================================================
-- Une tontine qui migre vers l'application a souvent déjà collecté une
-- partie du fond de caisse annuel (versé mensuellement ou en une fois au
-- bénéfice). On déclare, membre par membre et par année, ce qui a déjà été
-- versé :
--   * le montant est compté comme déjà payé : il réduit la retenue fond de
--     caisse appliquée au bénéfice du membre pour cette année ;
--   * il crédite le solde du fonds (journalisé en mouvement REPRISE, ou
--     REPRISE_CORRECTION quand une déclaration est revue à la baisse).

CREATE TABLE versements_anterieurs_fonds (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id  UUID          NOT NULL REFERENCES tontines(id) ON DELETE CASCADE,
    membre_id   UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    annee       SMALLINT      NOT NULL,
    montant     NUMERIC(15,2) NOT NULL CHECK (montant >= 0),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_versement_anterieur_membre_annee UNIQUE (membre_id, annee)
);
CREATE INDEX idx_versements_anterieurs_tontine ON versements_anterieurs_fonds(tontine_id, annee);

-- Le CHECK de V3 est anonyme : on retire tout CHECK portant sur type_mouvement.
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'mouvements_fonds_aide'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%type_mouvement%'
    LOOP
        EXECUTE format('ALTER TABLE mouvements_fonds_aide DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE mouvements_fonds_aide
    ADD CONSTRAINT mouvements_fonds_aide_type_mouvement_check
        CHECK (type_mouvement IN ('CONTRIBUTION', 'DECAISSEMENT', 'REPRISE', 'REPRISE_CORRECTION'));
