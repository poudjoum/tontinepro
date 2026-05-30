package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MembreEligibleRetardResponse(
        UUID membreId,
        String nom,
        String prenom,
        String matricule,
        int toursARattraper,
        BigDecimal montantCotisationUnitaire,
        BigDecimal montantRepasUnitaire,
        BigDecimal montantTotalRattrapage,
        List<DetailTourRattrapage> tours
) {
    public record DetailTourRattrapage(
            int numeroTour,
            String beneficiaireNom,
            String dateTour,
            BigDecimal cotisation,
            BigDecimal repas,
            BigDecimal total
    ) {}
}
