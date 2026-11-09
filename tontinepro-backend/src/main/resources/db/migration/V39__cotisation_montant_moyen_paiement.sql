-- Suivi des moyens de paiement des cotisations : cash vs mobile money
-- Permet au secrétaire de saisir comment chaque membre a payé,
-- afin de vérifier la caisse à la fin de la séance.

ALTER TABLE cotisations
    ADD COLUMN montant_cash NUMERIC(15,2) NULL,
    ADD COLUMN montant_mobile_money NUMERIC(15,2) NULL;
