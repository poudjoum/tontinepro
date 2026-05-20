package com.tontinepro.tontinepro_backend.api.tontine.dto;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTontineConfigRequest(

        String nom,

        String description,

        @DecimalMin("0.01")
        BigDecimal montantCotisationMin,

        @DecimalMin("0.01")
        BigDecimal montantCotisationMax,

        @DecimalMin("0.01")
        BigDecimal montantConsensuel,

        @Min(1) @Max(28)
        Integer jourReference,

        Tontine.TypeReglePeriodicite typeReglePeriodicite,

        LocalDate dateProchaineTontine,

        @DecimalMin("0.0")
        BigDecimal tauxInteretPret,

        @DecimalMin("0.0")
        BigDecimal tauxInteretEpargne,

        Boolean actif,

        Tontine.ModeContributionAide modeContributionAide,

        @DecimalMin("0.01")
        BigDecimal montantCotisationAide,

        @DecimalMin("0.0")
        BigDecimal montantAmende,

        @DecimalMin("0.0")
        BigDecimal montantPenaliteRetard,

        @DecimalMin("0.0")
        BigDecimal montantFondAideAnnuelMembre
) {}
