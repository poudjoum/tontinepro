package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ValiderBeneficeRequest(
        @NotNull
        @DecimalMin("0.00")
        BigDecimal montantRecu
) {}
