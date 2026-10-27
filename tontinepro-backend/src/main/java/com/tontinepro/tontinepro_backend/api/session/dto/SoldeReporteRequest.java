package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Caisse globale de la tontine à l'ouverture de la session. */
public record SoldeReporteRequest(@NotNull @PositiveOrZero BigDecimal montant) {}
