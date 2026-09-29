-- =====================================================================
-- Purge complète d'une (ou plusieurs) tontine(s) par motif de nom.
--
-- Usage (sur le serveur, dans /opt/tontinepro) :
--   docker exec -i tontinepro-postgres sh -c \
--     'psql -v ON_ERROR_STOP=1 -v motif=%LFD% -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
--     < scripts/purge-tontine.sql
--
-- Par sécurité le script se termine par ROLLBACK : il affiche ce qui
-- serait supprimé sans rien modifier. Une fois l'inventaire vérifié,
-- remplacer le ROLLBACK final par COMMIT et relancer.
--
-- Les comptes utilisateurs ne sont supprimés que s'ils n'appartiennent
-- à AUCUNE autre tontine et ne sont pas SUPER_ADMIN.
-- Les fichiers MinIO des documents ne sont pas supprimés (voir la liste
-- des chemins affichée plus bas).
-- =====================================================================

BEGIN;

-- ── Périmètre ────────────────────────────────────────────────────────
CREATE TEMP TABLE p_tontines ON COMMIT DROP AS
  SELECT id, nom FROM tontines WHERE nom ILIKE :'motif';

CREATE TEMP TABLE p_membres ON COMMIT DROP AS
  SELECT id, user_id FROM membres WHERE tontine_id IN (SELECT id FROM p_tontines);

CREATE TEMP TABLE p_sessions ON COMMIT DROP AS
  SELECT id FROM sessions_tontine WHERE tontine_id IN (SELECT id FROM p_tontines);

CREATE TEMP TABLE p_aides ON COMMIT DROP AS
  SELECT id FROM aides WHERE membre_id IN (SELECT id FROM p_membres);

CREATE TEMP TABLE p_fonds ON COMMIT DROP AS
  SELECT id FROM fonds_aide WHERE tontine_id IN (SELECT id FROM p_tontines);

CREATE TEMP TABLE p_users ON COMMIT DROP AS
  SELECT DISTINCT u.id, u.email, u.telephone, u.role
  FROM users u
  WHERE u.id IN (SELECT user_id FROM p_membres)
    AND u.role <> 'SUPER_ADMIN'
    AND NOT EXISTS (
      SELECT 1 FROM membres m
      WHERE m.user_id = u.id
        AND m.tontine_id NOT IN (SELECT id FROM p_tontines));

-- ── Inventaire ───────────────────────────────────────────────────────
\echo '=== Tontines ciblées ==='
SELECT id, nom FROM p_tontines;

\echo '=== Volumes ==='
SELECT 'membres' AS objet, count(*) FROM p_membres
UNION ALL SELECT 'sessions', count(*) FROM p_sessions
UNION ALL SELECT 'cotisations', count(*) FROM cotisations
          WHERE tontine_id IN (SELECT id FROM p_tontines) OR membre_id IN (SELECT id FROM p_membres)
UNION ALL SELECT 'aides', count(*) FROM p_aides
UNION ALL SELECT 'prets', count(*) FROM prets WHERE membre_id IN (SELECT id FROM p_membres)
UNION ALL SELECT 'documents membres', count(*) FROM documents WHERE membre_id IN (SELECT id FROM p_membres)
UNION ALL SELECT 'users supprimés', count(*) FROM p_users
UNION ALL SELECT 'users conservés (autre tontine / super admin)',
          count(DISTINCT user_id) - (SELECT count(*) FROM p_users) FROM p_membres;

\echo '=== Comptes utilisateurs qui seront supprimés ==='
SELECT email, telephone, role FROM p_users ORDER BY email;

\echo '=== Fichiers MinIO orphelins (à supprimer à la main si besoin) ==='
SELECT chemin_stockage FROM documents WHERE membre_id IN (SELECT id FROM p_membres)
UNION ALL
SELECT chemin_stockage FROM documents_tontine WHERE tontine_id IN (SELECT id FROM p_tontines);

-- ── Suppression (enfants d'abord) ────────────────────────────────────
DELETE FROM mouvements_fonds_aide
 WHERE fonds_aide_id IN (SELECT id FROM p_fonds)
    OR membre_id     IN (SELECT id FROM p_membres)
    OR aide_id       IN (SELECT id FROM p_aides);

DELETE FROM contributions_fonds_aide
 WHERE fonds_aide_id IN (SELECT id FROM p_fonds)
    OR membre_id     IN (SELECT id FROM p_membres)
    OR aide_id       IN (SELECT id FROM p_aides);

DELETE FROM versements_anterieurs_fonds
 WHERE tontine_id IN (SELECT id FROM p_tontines) OR membre_id IN (SELECT id FROM p_membres);

DELETE FROM documents WHERE membre_id IN (SELECT id FROM p_membres);

DELETE FROM absences
 WHERE tontine_id IN (SELECT id FROM p_tontines) OR membre_id IN (SELECT id FROM p_membres);

DELETE FROM sanctions
 WHERE tontine_id IN (SELECT id FROM p_tontines) OR membre_id IN (SELECT id FROM p_membres);

DELETE FROM part_lot
 WHERE membre_id IN (SELECT id FROM p_membres)
    OR ordre_beneficiaire_id IN (SELECT id FROM ordre_beneficiaires
                                 WHERE session_id IN (SELECT id FROM p_sessions));

DELETE FROM participation_lot
 WHERE session_id IN (SELECT id FROM p_sessions) OR membre_id IN (SELECT id FROM p_membres);

DELETE FROM ordre_beneficiaires
 WHERE session_id IN (SELECT id FROM p_sessions) OR membre_id IN (SELECT id FROM p_membres);

DELETE FROM cotisations
 WHERE tontine_id IN (SELECT id FROM p_tontines) OR membre_id IN (SELECT id FROM p_membres);

DELETE FROM sessions_tontine WHERE id IN (SELECT id FROM p_sessions);

DELETE FROM aides WHERE id IN (SELECT id FROM p_aides);

-- ayants_droit, comptes/mouvements épargne, prêts/échéances : ON DELETE CASCADE
DELETE FROM membres WHERE id IN (SELECT id FROM p_membres);

DELETE FROM invitation_tokens  WHERE tontine_id IN (SELECT id FROM p_tontines);
DELETE FROM demandes_adhesion  WHERE tontine_id IN (SELECT id FROM p_tontines);
DELETE FROM documents_tontine  WHERE tontine_id IN (SELECT id FROM p_tontines);
DELETE FROM rubriques_aide     WHERE tontine_id IN (SELECT id FROM p_tontines);
DELETE FROM fonds_aide         WHERE id IN (SELECT id FROM p_fonds);
DELETE FROM redevances         WHERE tontine_id IN (SELECT id FROM p_tontines);

DELETE FROM tontines WHERE id IN (SELECT id FROM p_tontines);

-- ── Comptes utilisateurs propres à la tontine ────────────────────────
-- Colonnes « traité/validé par » nullables : on détache plutôt que bloquer.
UPDATE aides             SET valide_par = NULL WHERE valide_par IN (SELECT id FROM p_users);
UPDATE prets             SET valide_par = NULL WHERE valide_par IN (SELECT id FROM p_users);
UPDATE demandes_adhesion SET traite_par = NULL WHERE traite_par IN (SELECT id FROM p_users);

DELETE FROM demandes_reinit_mdp WHERE email IN (SELECT email FROM p_users);

-- refresh_tokens, password_reset_tokens, notifications : ON DELETE CASCADE
DELETE FROM users WHERE id IN (SELECT id FROM p_users);

\echo '=== Vérification : tontines restantes correspondant au motif ==='
SELECT count(*) AS restantes FROM tontines WHERE nom ILIKE :'motif';

-- Remplacer par COMMIT une fois l'inventaire validé.
ROLLBACK;
