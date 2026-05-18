package com.tontinepro.tontinepro_backend.api.cotisation.dto;

import java.time.OffsetDateTime;

public record EnregistrerPaiementRequest(

        String referencePaiement,

        // si null, l'heure actuelle est utilisée
        OffsetDateTime datePaiement
) {}
