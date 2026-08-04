-- V29 – Délai de recouvrement des aides
-- =====================================================================
-- Chaque aide activée doit être entièrement recouvrée (collecte des parts
-- de tous les membres) dans un délai de 3 séances de tontine.
--
-- date_echeance_recouvrement : figée à l'activation = date du 3e tour à venir
--                              (OrdreBeneficiaire) après l'activation.
-- relance_recouvrement_envoyee : évite de renvoyer l'alerte de retard à
--                                chaque passage du job quotidien.

ALTER TABLE aides
    ADD COLUMN date_echeance_recouvrement DATE,
    ADD COLUMN relance_recouvrement_envoyee BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN aides.date_echeance_recouvrement IS
    'Date limite de recouvrement (3e seance suivant l''activation)';
