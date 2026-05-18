-- V7 – Fonction bureautique des membres dans la tontine
ALTER TABLE membres
    ADD COLUMN fonction VARCHAR(30) NOT NULL DEFAULT 'MEMBRE_ORDINAIRE'
        CHECK (fonction IN ('PRESIDENT', 'SECRETAIRE', 'TRESORIER', 'CENSEUR', 'MEMBRE_ORDINAIRE'));

CREATE INDEX idx_membres_fonction ON membres(fonction);
