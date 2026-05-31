-- Demandes de réinitialisation de mot de passe soumises par les membres
-- Le super admin voit ces demandes et envoie un lien via email

CREATE TABLE demandes_reinit_mdp (
    id          UUID            NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL,
    nom         VARCHAR(150)    NOT NULL,
    prenom      VARCHAR(150)    NOT NULL,
    telephone   VARCHAR(20),
    statut      VARCHAR(20)     NOT NULL DEFAULT 'EN_ATTENTE',  -- EN_ATTENTE | ENVOYEE
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_demandes_reinit_mdp_email ON demandes_reinit_mdp (email);
CREATE INDEX idx_demandes_reinit_mdp_statut ON demandes_reinit_mdp (statut);
