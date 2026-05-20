-- V16 — Super Admin : rôle, réinitialisation mot de passe, redevances

-- Ajouter SUPER_ADMIN à la contrainte de rôle
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'SECRETAIRE', 'MEMBRE', 'INVITE'));

-- Tokens de réinitialisation de mot de passe (24h, usage unique)
CREATE TABLE password_reset_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_prt_token   ON password_reset_tokens(token);
CREATE INDEX idx_prt_user_id ON password_reset_tokens(user_id);

-- Redevances (abonnement mensuel ou annuel par tontine)
CREATE TABLE redevances (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id        UUID        NOT NULL UNIQUE REFERENCES tontines(id) ON DELETE CASCADE,
    montant           NUMERIC(15,2) NOT NULL DEFAULT 0,
    periodicite       VARCHAR(10) NOT NULL DEFAULT 'MENSUEL'
                      CHECK (periodicite IN ('MENSUEL', 'ANNUEL')),
    statut            VARCHAR(20) NOT NULL DEFAULT 'A_JOUR'
                      CHECK (statut IN ('A_JOUR', 'EN_RETARD', 'GRATUIT')),
    prochain_paiement DATE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
