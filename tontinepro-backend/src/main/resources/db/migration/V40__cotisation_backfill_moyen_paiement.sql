-- Remplir les montants par moyen de paiement pour les cotisations existantes.
-- Pour les cotisations déjà payées où montant_cash et montant_mobile_money sont NULL,
-- on initialise montant_cash au total payé (montant + montant_fond_aide + montant_repas)
-- et montant_mobile_money à 0, car le paiement a été enregistré avant la distinction
-- entre les moyens de paiement.

UPDATE cotisations
SET
    montant_cash = montant
        + COALESCE(montant_fond_aide, 0)
        + COALESCE(montant_repas, 0),
    montant_mobile_money = 0
WHERE statut = 'PAYEE'
  AND montant_cash IS NULL
  AND montant_mobile_money IS NULL;
