-- V9 — Documents membres (CNI, lettre d'engagement, etc.)

CREATE TABLE documents (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    membre_id       UUID         NOT NULL REFERENCES membres(id),
    type_document   VARCHAR(30)  NOT NULL
        CHECK (type_document IN ('CNI', 'LETTRE_ENGAGEMENT', 'JUSTIFICATIF_ABSENCE', 'AUTRE')),
    nom_fichier     VARCHAR(255) NOT NULL,
    chemin_stockage VARCHAR(500) NOT NULL,
    taille_octets   BIGINT       NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_documents_membre ON documents(membre_id);
