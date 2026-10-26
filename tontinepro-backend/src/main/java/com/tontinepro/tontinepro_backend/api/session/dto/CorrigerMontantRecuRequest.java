package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Correction du montant réellement remis pour un tour déjà payé (avance comprise). */
public record CorrigerMontantRecuRequest(@NotNull @PositiveOrZero BigDecimal montantRecu) {}
