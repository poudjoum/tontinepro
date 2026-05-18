-- V4 – Créer un compte épargne pour les membres existants qui n'en ont pas encore.
-- Les nouveaux membres en auront un dès leur création via MembreService.
INSERT INTO comptes_epargne (membre_id)
SELECT m.id
FROM membres m
WHERE NOT EXISTS (
    SELECT 1 FROM comptes_epargne ce WHERE ce.membre_id = m.id
);
