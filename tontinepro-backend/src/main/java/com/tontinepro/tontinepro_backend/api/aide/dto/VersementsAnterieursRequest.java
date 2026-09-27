package com.tontinepro.tontinepro_backend.api.aide.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Déclaration du fond de caisse versé avant l'application. Chaque ligne fixe
 * le montant d'un membre pour l'année (0 efface la déclaration) ; les membres
 * absents de la liste ne sont pas modifiés.
 */
public record VersementsAnterieursRequest(
        @NotNull Short annee,
        @NotNull @Valid List<Ligne> lignes
) {
    public record Ligne(
            @NotNull UUID membreId,
            @NotNull @DecimalMin("0") BigDecimal montant
    ) {}
}
