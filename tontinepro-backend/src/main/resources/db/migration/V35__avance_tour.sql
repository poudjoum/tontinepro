-- =====================================================================
-- V35 - Avance versee sur un tour pas encore paye (tours tenus a
-- l'ancienne). Une fois le tour paye, l'avance est comprise dans
-- montant_recu.
-- =====================================================================
ALTER TABLE ordre_beneficiaires
    ADD COLUMN montant_avance NUMERIC(15,2) NULL,
    ADD COLUMN date_avance    DATE          NULL;
