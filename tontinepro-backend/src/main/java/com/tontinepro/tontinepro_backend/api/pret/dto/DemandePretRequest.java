package com.tontinepro.tontinepro_backend.api.pret.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DemandePretRequest(

        @NotNull(message = "Le montant principal est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        BigDecimal montantPrincipal,

        @NotNull(message = "La durée est obligatoire")
        @Min(value = 1, message = "La durée minimale est 1 mois")
        @Max(value = 60, message = "La durée maximale est 60 mois")
        Short dureeMois
) {}
