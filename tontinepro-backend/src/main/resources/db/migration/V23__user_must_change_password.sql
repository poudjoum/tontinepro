-- Mot de passe temporaire imposé lors de l'import de membres :
-- l'utilisateur doit obligatoirement le changer à sa première connexion.
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
