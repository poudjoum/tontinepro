package com.tontinepro.tontinepro_backend.api.tontine.dto;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TontineResponse(

        UUID id,
        String nom,
        String description,
        BigDecimal montantCotisationMin,
        BigDecimal montantCotisationMax,
        BigDecimal montantConsensuel,
        Integer jourReference,
        Tontine.TypeReglePeriodicite typeReglePeriodicite,
        LocalDate dateProchaineTontine,
        BigDecimal tauxInteretPret,
        BigDecimal tauxInteretEpargne,
        Tontine.ModeContributionAide modeContributionAide,
        BigDecimal montantCotisationAide,
        BigDecimal montantAmende,
        BigDecimal montantPenaliteRetard,
        BigDecimal montantFondAideAnnuelMembre,
        Tontine.TypeAcces typeAcces,
        boolean visible,
        String descriptionAcces,
        boolean actif,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TontineResponse from(Tontine t) {
        return new TontineResponse(
                t.getId(), t.getNom(), t.getDescription(),
                t.getMontantCotisationMin(), t.getMontantCotisationMax(), t.getMontantConsensuel(),
                t.getJourReference(), t.getTypeReglePeriodicite(), t.getDateProchaineTontine(),
                t.getTauxInteretPret(), t.getTauxInteretEpargne(),
                t.getModeContributionAide(), t.getMontantCotisationAide(),
                t.getMontantAmende(), t.getMontantPenaliteRetard(),
                t.getMontantFondAideAnnuelMembre(),
                t.getTypeAcces(), t.isVisible(), t.getDescriptionAcces(),
                t.isActif(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
