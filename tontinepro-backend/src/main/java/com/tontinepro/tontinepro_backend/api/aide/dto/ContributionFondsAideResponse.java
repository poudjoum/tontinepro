package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContributionFondsAideResponse(

        UUID id,
        UUID membreId,
        String membreMatricule,
        String membreNom,
        String membrePrenom,
        BigDecimal montant,
        Short mois,
        Short annee,
        UUID aideId,
        ContributionFondsAide.Statut statut,
        OffsetDateTime datePaiement,
        OffsetDateTime createdAt
) {
    public static ContributionFondsAideResponse from(ContributionFondsAide c) {
        return new ContributionFondsAideResponse(
                c.getId(),
                c.getMembre().getId(),
                c.getMembre().getMatricule(),
                c.getMembre().getNom(),
                c.getMembre().getPrenom(),
                c.getMontant(),
                c.getMois(),
                c.getAnnee(),
                c.getAide() != null ? c.getAide().getId() : null,
                c.getStatut(),
                c.getDatePaiement(),
                c.getCreatedAt()
        );
    }
}