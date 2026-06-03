package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/** Inscription d'un membre à une session « à lot » avec sa mise mensuelle. */
public record AdhererLotRequest(
        @NotNull UUID membreId,
        @NotNull @DecimalMin("0.01") BigDecimal montantMensuel
) {}
