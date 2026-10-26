package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Remboursement (partiel ou total) de la dette d'un membre parti après son tour. */
public record RemboursementDetteRequest(@NotNull @DecimalMin("0.01") BigDecimal montant) {}
