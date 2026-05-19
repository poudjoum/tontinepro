package com.tontinepro.tontinepro_backend.api.sanction.dto;

import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreerSanctionRequest(

        @NotNull
        UUID membreId,

        @NotNull
        Sanction.TypeSanction typeSanction,

        @NotNull @DecimalMin("0.01")
        BigDecimal montant,

        String motif
) {}
