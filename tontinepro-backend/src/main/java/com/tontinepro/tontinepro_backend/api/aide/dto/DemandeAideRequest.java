package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DemandeAideRequest(

        @NotNull(message = "Le type d'aide est obligatoire")
        Aide.TypeAide typeAide,

        @NotNull(message = "Le montant demandé est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        BigDecimal montantDemande,

        @NotBlank(message = "Le motif est obligatoire")
        String motif,

        String justificatifUrl
) {}