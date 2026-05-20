-- V14 — Un utilisateur peut appartenir à plusieurs tontines
--        Supprime l'unicité sur user_id seul,
--        ajoute une contrainte composite (user_id, tontine_id)

-- Supprimer l'ancienne contrainte UNIQUE sur user_id seul
ALTER TABLE membres DROP CONSTRAINT IF EXISTS membres_user_id_key;

-- Un utilisateur ne peut être inscrit qu'une seule fois par tontine
ALTER TABLE membres
  ADD CONSTRAINT uq_membres_user_tontine UNIQUE (user_id, tontine_id);
