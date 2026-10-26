-- =====================================================================
-- V32 – Tontine à lot : versement de chaque part d'un tour séparément
-- (un lot partagé 10 000 + 10 000 + 30 000 = trois versements, chacun
-- avec sa propre retenue du fond de caisse).
-- =====================================================================
ALTER TABLE part_lot
    ADD COLUMN verse           BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN date_versement  DATE          NULL,
    ADD COLUMN montant_retenue NUMERIC(15,2) NULL,
    ADD COLUMN montant_verse   NUMERIC(15,2) NULL,
    ADD COLUMN annee_retenue   SMALLINT      NULL;

-- Tours déjà validés avant cette version : leurs parts sont considérées versées
-- (sans retenue individuelle, celle-ci n'était pas calculée par part).
UPDATE part_lot pl
SET verse = TRUE,
    montant_verse = pl.part_cagnotte,
    montant_retenue = 0,
    date_versement = COALESCE(ob.date_benefice, CURRENT_DATE)
FROM ordre_beneficiaires ob
WHERE ob.id = pl.ordre_beneficiaire_id
  AND ob.beneficie = TRUE;
