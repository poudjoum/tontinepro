package com.tontinepro.tontinepro_backend.api.tontine.dto;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreerMaTontineRequest(

        @NotBlank
        String nom,

        String description,

        @NotNull @DecimalMin("0.01")
        BigDecimal montantCotisationMin,

        Tontine.TypeAcces typeAcces,

        String descriptionAcces,

        @NotBlank
        String membreNom,

        @NotBlank
        String membrePrenom
) {}
