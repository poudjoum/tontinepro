package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AideResponse(

        UUID id,
        Aide.TypeAide typeAide,
        UUID rubriqueId,
        String rubriqueLibelle,
        String variante,
        BigDecimal montantDemande,
        BigDecimal montantAccorde,
        // Snapshot d'activation (aides issues du barème)
        RubriqueAide.ModeCalcul modeCalcul,
        Integer nbMembresBase,
        BigDecimal partParMembre,
        boolean prefinance,
        String motif,
        Aide.Statut statut,
        String justificatifUrl,
        String motifRejet,
        UUID membreId,
        String membreMatricule,
        String membreNom,
        String membrePrenom,
        UUID valideParId,
        String valideParEmail,
        OffsetDateTime dateValidation,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AideResponse from(Aide a) {
        return new AideResponse(
                a.getId(),
                a.getTypeAide(),
                a.getRubrique() != null ? a.getRubrique().getId() : null,
                a.getRubrique() != null ? a.getRubrique().getLibelle() : null,
                a.getVariante(),
                a.getMontantDemande(),
                a.getMontantAccorde(),
                a.getModeCalcul(),
                a.getNbMembresBase(),
                a.getPartParMembre(),
                a.isPrefinance(),
                a.getMotif(),
                a.getStatut(),
                a.getJustificatifUrl(),
                a.getMotifRejet(),
                a.getMembre().getId(),
                a.getMembre().getMatricule(),
                a.getMembre().getNom(),
                a.getMembre().getPrenom(),
                a.getValidePar() != null ? a.getValidePar().getId() : null,
                a.getValidePar() != null ? a.getValidePar().getEmail() : null,
                a.getDateValidation(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}