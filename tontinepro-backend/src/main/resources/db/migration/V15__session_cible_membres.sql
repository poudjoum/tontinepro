-- V15 — Cible de membres pour la session (nombre visé avant démarrage)
ALTER TABLE sessions_tontine
  ADD COLUMN IF NOT EXISTS cible_membres INTEGER;
