package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaisirPaiementsSeanceRequest(
        @NotNull List<PaiementMembre> paiements,
        /**
         * Si vrai, les cotisations déjà PAYEE sont écrasées (correction).
         * Utilisé par la reprise de tontine pour re-saisir des tours passés.
         * Par défaut (null/false) les cotisations PAYEE sont ignorées.
         */
        Boolean autoriserCorrection
) {
    public record PaiementMembre(
            @NotNull UUID cotisationId,
            String referencePaiement,
            BigDecimal montantTontine,
            BigDecimal montantFondAide,
            BigDecimal montantRepas,
            OffsetDateTime datePaiement,
            com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation.MoyenPaiement moyenPaiement,
            /** Contributions d'aide (parts) à encaisser pour ce membre pendant la séance. */
            List<UUID> partsAidePayees
    ) {}
}
