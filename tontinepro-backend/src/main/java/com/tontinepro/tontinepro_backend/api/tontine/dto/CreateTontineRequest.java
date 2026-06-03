package com.tontinepro.tontinepro_backend.api.tontine.dto;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTontineRequest(

        @NotBlank
        String nom,

        String description,

        @NotNull @DecimalMin("0.01")
        BigDecimal montantCotisationMin,

        @DecimalMin("0.01")
        BigDecimal montantCotisationMax,

        @DecimalMin("0.01")
        BigDecimal montantConsensuel,

        @Min(1) @Max(28)
        Integer jourReference,

        Tontine.TypeReglePeriodicite typeReglePeriodicite,

        LocalDate dateProchaineTontine,

        @NotNull @DecimalMin("0.0")
        BigDecimal tauxInteretPret,

        @NotNull @DecimalMin("0.0")
        BigDecimal tauxInteretEpargne,

        Tontine.ModeContributionAide modeContributionAide,

        @DecimalMin("0.01")
        BigDecimal montantCotisationAide,

        /** Mode de la tontine (CLASSIQUE par défaut, ou A_LOT). */
        Tontine.ModeTontine mode,

        /** Montant d'un lot (requis si mode = A_LOT). */
        @DecimalMin("0.01")
        BigDecimal montantLot,

        /** Mois de clôture des adhésions (mode A_LOT, défaut 3). */
        @Min(1) @Max(12)
        Integer moisClotureAdhesions
) {}
