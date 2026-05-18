package com.tontinepro.tontinepro_backend.api.aide.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValiderAideRequest(

        @NotNull(message = "Le montant accordé est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant accordé doit être supérieur à 0")
        BigDecimal montantAccorde
) {}