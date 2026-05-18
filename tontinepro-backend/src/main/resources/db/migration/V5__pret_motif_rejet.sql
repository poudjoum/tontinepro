-- V5 – Ajout de motif_rejet sur la table prets
ALTER TABLE prets
    ADD COLUMN motif_rejet TEXT;
