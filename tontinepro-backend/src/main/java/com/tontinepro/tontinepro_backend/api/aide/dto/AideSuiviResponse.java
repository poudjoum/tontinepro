package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Suivi d'une aide activée : avancement de la collecte des contributions
 * et impact sur la trésorerie du fonds.
 */
public record AideSuiviResponse(

        UUID aideId,
        String rubriqueLibelle,
        String beneficiaireNom,
        String beneficiaireMatricule,
        Aide.Statut statut,
        boolean prefinance,

        BigDecimal montantTotal,      // total versé/dû au bénéficiaire
        BigDecimal partParMembre,
        int nbMembresBase,

        BigDecimal totalAttendu,      // Σ contributions
        BigDecimal totalCollecte,     // Σ contributions PAYEE
        int nbPayes,
        int nbTotal,

        BigDecimal soldeFonds,        // solde courant du fonds d'aide

        java.util.List<LigneContribution> contributions
) {
    public record LigneContribution(
            UUID contributionId,
            UUID membreId,
            String membreNom,
            String membreMatricule,
            BigDecimal montant,
            ContributionFondsAide.Statut statut,
            OffsetDateTime datePaiement,
            boolean estBeneficiaire
    ) {}
}
