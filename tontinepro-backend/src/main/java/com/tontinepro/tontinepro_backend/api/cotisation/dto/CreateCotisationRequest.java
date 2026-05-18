package com.tontinepro.tontinepro_backend.api.cotisation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCotisationRequest(

        @NotNull
        UUID membreId,

        @NotNull @Min(1) @Max(12)
        Short mois,

        @NotNull @Min(2000) @Max(2100)
        Short annee,

        // si null, le montant de cotisation de la tontine est utilisé
        @Min(0)
        BigDecimal montant
) {}
