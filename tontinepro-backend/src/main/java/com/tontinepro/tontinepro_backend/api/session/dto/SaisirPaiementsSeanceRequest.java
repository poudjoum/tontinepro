package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaisirPaiementsSeanceRequest(
        @NotNull List<PaiementMembre> paiements
) {
    public record PaiementMembre(
            @NotNull UUID cotisationId,
            String referencePaiement,
            BigDecimal montantTontine,
            BigDecimal montantFondAide,
            BigDecimal montantRepas,
            OffsetDateTime datePaiement
    ) {}
}
