-- Type de participation d'un membre dans une tontine
-- TONTINE      : participation complète (calendrier, cotisation + fond + repas)
-- AIDE_SOCIALE : uniquement le fond de solidarité, pas de slot dans le calendrier

ALTER TABLE membres
    ADD COLUMN type_participation VARCHAR(20) NOT NULL DEFAULT 'TONTINE',
    ADD COLUMN mode_paiement_aide VARCHAR(20) NULL;   -- MENSUEL | EN_UNE_FOIS (uniquement si AIDE_SOCIALE)

-- Stocker le type de participation dans les demandes d'adhésion
ALTER TABLE demandes_adhesion
    ADD COLUMN type_participation VARCHAR(20) NULL DEFAULT 'TONTINE',
    ADD COLUMN mode_paiement_aide VARCHAR(20) NULL;
