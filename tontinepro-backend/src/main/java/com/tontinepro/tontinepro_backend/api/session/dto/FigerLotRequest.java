package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Composition des lots retenue par le bureau au moment du figeage. Chaque lot doit
 * valoir exactement le montant du lot et chaque membre y placer exactement sa mise.
 * Corps absent ou sans lots → composition automatique.
 */
public record FigerLotRequest(@Valid List<LotDemande> lots) {

    public record LotDemande(@NotEmpty @Valid List<PartDemandee> parts) {}

    public record PartDemandee(@NotNull UUID membreId, @NotNull BigDecimal montant) {}
}
