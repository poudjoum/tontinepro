package com.tontinepro.tontinepro_backend.api.setup.dto;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SetupRequest(

        // ── Compte fondateur (Président) ────────────────────────────────
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String telephone,

        // ── Configuration de la tontine ──────────────────────────────────
        @NotBlank String tontineNom,
        String tontineDescription,

        @NotNull @DecimalMin("0.01")
        BigDecimal montantCotisation,

        @NotNull @Min(1) @Max(28)
        Short jourCotisation,

        @DecimalMin("0.0") BigDecimal tauxInteretPret,
        @DecimalMin("0.0") BigDecimal tauxInteretEpargne,

        Tontine.ModeContributionAide modeContributionAide,

        @DecimalMin("0.01") BigDecimal montantCotisationAide
) {}
