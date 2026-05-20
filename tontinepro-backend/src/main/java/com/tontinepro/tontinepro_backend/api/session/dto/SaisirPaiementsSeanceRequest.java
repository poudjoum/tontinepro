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
            /** Référence de la transaction (preuve de paiement). */
            String referencePaiement,
            /** Montant cotisation tontine. Si null → montant déjà sur la cotisation. */
            BigDecimal montantTontine,
            /** Montant fonds d'aide du mois. Si null → 0. */
            BigDecimal montantFondAide,
            /** Date du paiement. Si null → maintenant. */
            OffsetDateTime datePaiement
    ) {}
}
