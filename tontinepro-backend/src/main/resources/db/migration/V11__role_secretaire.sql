-- V11 — Ajout du rôle SECRETAIRE pour la gestion quotidienne de la tontine

-- Élargir la contrainte CHECK du rôle utilisateur pour inclure SECRETAIRE
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'SECRETAIRE', 'MEMBRE', 'INVITE'));

-- Migrer le compte fondateur (PRESIDENT -> SECRETAIRE si rôle ADMIN actuel)
-- On ne touche pas aux comptes existants car le setup est déjà fait ;
-- les nouveaux comptes utilisent désormais le rôle SECRETAIRE directement.
