package com.tontinepro.tontinepro_backend.api.tontine.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateTontineRequest(

        @NotBlank
        String nom,

        String description,

        @NotNull @DecimalMin("0.01")
        BigDecimal montantCotisation,

        @NotNull @Min(1) @Max(28)
        Short jourCotisation,

        @NotNull @DecimalMin("0.0")
        BigDecimal tauxInteretPret,

        @NotNull @DecimalMin("0.0")
        BigDecimal tauxInteretEpargne
) {}
