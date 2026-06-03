package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Saisie groupée des cotisations d'un mois passé lors d'une reprise de tontine
 * (rattrapage). Contrairement à la saisie de séance classique, cet endpoint
 * CRÉE la cotisation si elle n'existe pas encore (membre sans ligne pour ce mois),
 * indépendamment du statut actuel du membre — car il s'agit d'historique.
 */
public record SaisirRattrapageRequest(
        @NotNull List<LigneRattrapage> lignes
) {
    public record LigneRattrapage(
            @NotNull UUID membreId,
            BigDecimal montantTontine,
            BigDecimal montantFondAide,
            BigDecimal montantRepas,
            String referencePaiement
    ) {}
}
