package com.tontinepro.tontinepro_backend.api.cotisation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EnregistrerPaiementRequest(

        String referencePaiement,

        /** Part tontine (contribue au pot du bénéficiaire). Si null → montantCotisationMin de la tontine. */
        BigDecimal montantTontine,

        /** Part fonds d'aide de ce mois. Si null → 0. */
        BigDecimal montantFondAide,

        // si null, l'heure actuelle est utilisée
        OffsetDateTime datePaiement
) {}
