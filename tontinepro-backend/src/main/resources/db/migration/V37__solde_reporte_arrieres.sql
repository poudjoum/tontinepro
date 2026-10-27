-- =====================================================================
-- V37 - Solde reporte et arrieres de fonds
--   * solde_reporte : caisse globale de la tontine a l'ouverture de la
--     session (argent detenu avant, toutes rubriques confondues), saisie
--     par le bureau et reprise dans le rapport de fin de session.
--   * arrieres_membres : fonds non cotise par un membre avant
--     l'application. Chaque reglement credite le fonds d'aide et est
--     journalise (ARRIERE, ou ARRIERE_CORRECTION quand on l'annule).
-- =====================================================================
ALTER TABLE sessions_tontine
    ADD COLUMN solde_reporte NUMERIC(15,2) NOT NULL DEFAULT 0;

CREATE TABLE arrieres_membres (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tontine_id  UUID          NOT NULL REFERENCES tontines(id) ON DELETE CASCADE,
    membre_id   UUID          NOT NULL REFERENCES membres(id) ON DELETE CASCADE,
    libelle     VARCHAR(200)  NOT NULL,
    montant     NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    date_constat DATE         NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_arrieres_membres_tontine ON arrieres_membres(tontine_id);

CREATE TABLE reglements_arrieres (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    arriere_id     UUID          NOT NULL REFERENCES arrieres_membres(id) ON DELETE CASCADE,
    montant        NUMERIC(15,2) NOT NULL CHECK (montant > 0),
    date_reglement DATE          NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reglements_arrieres_arriere ON reglements_arrieres(arriere_id);

ALTER TABLE mouvements_fonds_aide DROP CONSTRAINT mouvements_fonds_aide_type_mouvement_check;
ALTER TABLE mouvements_fonds_aide
    ADD CONSTRAINT mouvements_fonds_aide_type_mouvement_check
        CHECK (type_mouvement IN ('CONTRIBUTION', 'DECAISSEMENT', 'REPRISE', 'REPRISE_CORRECTION',
                                  'ARRIERE', 'ARRIERE_CORRECTION'));
