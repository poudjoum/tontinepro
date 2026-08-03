-- V28 – Règles d'éligibilité et variantes sur le barème des aides
-- =====================================================================
-- limite_par_beneficiaire : nb max de fois qu'un membre peut en bénéficier
--                           (NULL = illimité).
-- portee_limite           : fenêtre d'application de la limite.
-- variantes               : sous-choix optionnels (ex. « Père,Mère ») ; la limite
--                           s'applique alors par variante.
-- aides.variante          : variante choisie pour la demande.

ALTER TABLE rubriques_aide
    ADD COLUMN limite_par_beneficiaire INTEGER
        CHECK (limite_par_beneficiaire IS NULL OR limite_par_beneficiaire > 0),
    ADD COLUMN portee_limite VARCHAR(20) NOT NULL DEFAULT 'VIE'
        CHECK (portee_limite IN ('VIE', 'SESSION', 'ANNEE')),
    ADD COLUMN variantes TEXT;

ALTER TABLE aides
    ADD COLUMN variante VARCHAR(60);
