package com.tontinepro.tontinepro_backend.api.cotisation.dto;

import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CotisationResponse(

        UUID id,
        short mois,
        short annee,
        BigDecimal montant,
        BigDecimal montantFondAide,
        BigDecimal montantRepas,
        BigDecimal montantTotal,
        Cotisation.Statut statut,
        OffsetDateTime datePaiement,
        String referencePaiement,
        Cotisation.MoyenPaiement moyenPaiement,
        UUID membreId,
        String membreMatricule,
        String membreNom,
        String membrePrenom,
        UUID tontineId,
        String tontineNom,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CotisationResponse from(Cotisation c) {
        BigDecimal fondAide = c.getMontantFondAide() != null ? c.getMontantFondAide() : BigDecimal.ZERO;
        BigDecimal repas    = c.getMontantRepas()    != null ? c.getMontantRepas()    : BigDecimal.ZERO;
        return new CotisationResponse(
                c.getId(),
                c.getMois(),
                c.getAnnee(),
                c.getMontant(),
                fondAide,
                repas,
                c.getMontant().add(fondAide).add(repas),
                c.getStatut(),
                c.getDatePaiement(),
                c.getReferencePaiement(),
                c.getMoyenPaiement(),
                c.getMembre().getId(),
                c.getMembre().getMatricule(),
                c.getMembre().getNom(),
                c.getMembre().getPrenom(),
                c.getTontine().getId(),
                c.getTontine().getNom(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
