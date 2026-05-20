-- V13 — Cotisation enrichie : montantTontine + montantFondAide, fonds annuel membre

-- Montant annuel du fonds d'aide par membre (configurable par le secrétaire)
ALTER TABLE tontines
  ADD COLUMN IF NOT EXISTS montant_fond_aide_annuel_membre NUMERIC(15,2);

-- Décomposition de la cotisation : part tontine + part fonds d'aide
ALTER TABLE cotisations
  ADD COLUMN IF NOT EXISTS montant_fond_aide NUMERIC(15,2) NOT NULL DEFAULT 0;

-- Backfill : toutes les cotisations existantes ont montantFondAide = 0
-- (leur montant actuel = montantTontine uniquement)
