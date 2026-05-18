package com.tontinepro.tontinepro_backend.api.tontine.dto;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateTontineConfigRequest(

        String nom,

        String description,

        @DecimalMin("0.01")
        BigDecimal montantCotisation,

        @Min(1) @Max(28)
        Short jourCotisation,

        @DecimalMin("0.0")
        BigDecimal tauxInteretPret,

        @DecimalMin("0.0")
        BigDecimal tauxInteretEpargne,

        Boolean actif,

        Tontine.ModeContributionAide modeContributionAide,

        @DecimalMin("0.01")
        BigDecimal montantCotisationAide
) {}
