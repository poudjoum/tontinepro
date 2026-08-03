-- V27 – Nouveaux statuts d'aide : saisie par le bureau + consentement du membre
-- =====================================================================
-- PROPOSEE : aide saisie par le secrétaire/admin au nom d'un membre, en attente
--            de l'accord de ce membre.
-- REFUSEE  : le membre a refusé la proposition.

ALTER TABLE aides DROP CONSTRAINT IF EXISTS aides_statut_check;

ALTER TABLE aides ADD CONSTRAINT aides_statut_check
    CHECK (statut IN ('PROPOSEE', 'SOUMISE', 'VALIDEE', 'REJETEE', 'PAYEE', 'REFUSEE'));
