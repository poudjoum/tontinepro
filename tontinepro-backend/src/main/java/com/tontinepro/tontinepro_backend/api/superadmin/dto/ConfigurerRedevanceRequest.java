package com.tontinepro.tontinepro_backend.api.superadmin.dto;

import com.tontinepro.tontinepro_backend.domain.redevance.Redevance;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfigurerRedevanceRequest(
        @NotNull @PositiveOrZero BigDecimal montant,
        @NotNull Redevance.Periodicite periodicite,
        @NotNull Redevance.Statut statut,
        LocalDate prochainPaiement
) {}
