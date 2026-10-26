package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tour de remboursement : sa date et les membres cochés par le bureau, avec ce que chacun verse. */
public record TourRemboursementRequest(@NotNull LocalDate dateTour,
                                       @NotEmpty List<@Valid Cotisant> cotisants) {

    public record Cotisant(@NotNull UUID membreId, @NotNull @DecimalMin("0.01") BigDecimal montant) {}
}
