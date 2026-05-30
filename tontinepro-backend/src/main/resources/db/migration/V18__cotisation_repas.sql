-- Ajout du montant repas sur les cotisations
ALTER TABLE cotisations
    ADD COLUMN montant_repas NUMERIC(15, 2) NOT NULL DEFAULT 0;