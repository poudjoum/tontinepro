-- Suivi des moyens de paiement pour les versements aux bénéficiaires
-- Permet de détailler cash / Orange Money / MTN MoMo pour faciliter la vérification de caisse

-- Tontine classique : détail du versement au bénéficiaire du tour
ALTER TABLE ordre_beneficiaires
    ADD COLUMN montant_cash NUMERIC(15,2) NULL,
    ADD COLUMN montant_om NUMERIC(15,2) NULL,
    ADD COLUMN montant_momo NUMERIC(15,2) NULL,
    ADD COLUMN ref_om VARCHAR(100) NULL,
    ADD COLUMN ref_momo VARCHAR(100) NULL;

-- Aides mutuelles : détail du versement au bénéficiaire
ALTER TABLE aides
    ADD COLUMN montant_cash NUMERIC(15,2) NULL,
    ADD COLUMN montant_om NUMERIC(15,2) NULL,
    ADD COLUMN montant_momo NUMERIC(15,2) NULL,
    ADD COLUMN ref_om VARCHAR(100) NULL,
    ADD COLUMN ref_momo VARCHAR(100) NULL;
