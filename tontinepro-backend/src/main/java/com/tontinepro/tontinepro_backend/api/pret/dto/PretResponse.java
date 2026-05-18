package com.tontinepro.tontinepro_backend.api.pret.dto;

import com.tontinepro.tontinepro_backend.domain.pret.Pret;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PretResponse(

        UUID id,
        UUID membreId,
        String membreMatricule,
        String membreNom,
        String membrePrenom,
        BigDecimal montantPrincipal,
        BigDecimal tauxInteret,
        short dureeMois,
        BigDecimal montantTotal,
        BigDecimal totalInterets,
        Pret.Statut statut,
        String motifRejet,
        UUID valideParId,
        String valideParEmail,
        OffsetDateTime dateValidation,
        OffsetDateTime dateDecaissement,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PretResponse from(Pret p) {
        return new PretResponse(
                p.getId(),
                p.getMembre().getId(),
                p.getMembre().getMatricule(),
                p.getMembre().getNom(),
                p.getMembre().getPrenom(),
                p.getMontantPrincipal(),
                p.getTauxInteret(),
                p.getDureeMois(),
                p.getMontantTotal(),
                p.getMontantTotal().subtract(p.getMontantPrincipal()),
                p.getStatut(),
                p.getMotifRejet(),
                p.getValidePar() != null ? p.getValidePar().getId() : null,
                p.getValidePar() != null ? p.getValidePar().getEmail() : null,
                p.getDateValidation(),
                p.getDateDecaissement(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
