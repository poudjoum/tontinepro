package com.tontinepro.tontinepro_backend.api.aide.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Nouvel arriéré : le membre, ce qu'il doit, et pourquoi (par défaut « Fonds non cotisé »). */
public record ArriereRequest(
        @NotNull UUID membreId,
        @Size(max = 200) String libelle,
        @NotNull @DecimalMin("0.01") BigDecimal montant,
        LocalDate dateConstat
) {}
