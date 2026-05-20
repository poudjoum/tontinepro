package com.tontinepro.tontinepro_backend.api.session.dto;

import java.util.List;
import java.util.UUID;

public record SessionCotisationsStatutResponse(
        UUID sessionId,
        int sessionNumero,
        int mois,
        int annee,
        int totalMembres,
        int nbPayes,
        int nbEnAttente,
        int nbEnRetard,
        int nbAbsents,
        boolean complete,
        /** Montant cotisation tontine par défaut (montantCotisationMin de la tontine). */
        java.math.BigDecimal montantCotisationDefaut,
        /** Montant fond d'aide mensuel par défaut (null si mode != MENSUEL). */
        java.math.BigDecimal montantFondAideDefaut,
        /** Mode de contribution au fond d'aide. */
        String modeContributionAide,
        List<MembreCotisationStatut> membres
) {
    public record MembreCotisationStatut(
            UUID membreId,
            String nom,
            String prenom,
            String matricule,
            String statutCotisation,
            UUID cotisationId
    ) {}
}
