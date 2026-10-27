package com.tontinepro.tontinepro_backend.api.aide.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Règlement d'un arriéré encaissé par le bureau (date du jour si absente). */
public record ReglementArriereRequest(
        @NotNull @DecimalMin("0.01") BigDecimal montant,
        LocalDate dateReglement
) {}
