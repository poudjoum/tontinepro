package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Création / mise à jour d'une rubrique du barème d'aide.
 * Les champs booléens sont des objets (nullables) pour distinguer « non fourni »
 * lors d'une mise à jour partielle.
 */
public record RubriqueAideRequest(

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        Aide.TypeAide typeAide,

        @NotNull(message = "Le mode de calcul est obligatoire")
        RubriqueAide.ModeCalcul modeCalcul,

        @NotNull(message = "Le montant de référence est obligatoire")
        @DecimalMin(value = "0.0", inclusive = false, message = "Le montant doit être positif")
        BigDecimal montantReference,

        Boolean prefinancable,
        Boolean actif,
        String description
) {}
