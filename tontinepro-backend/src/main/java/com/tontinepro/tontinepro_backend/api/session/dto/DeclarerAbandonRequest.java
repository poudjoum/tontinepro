package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Abandon d'un membre en cours de session à lot (date du jour si absente).
 * Mois et montants : saisis par le bureau, obligatoires tant que la session n'est pas
 * figée ; {@code null} = calculés par l'application.
 */
public record DeclarerAbandonRequest(
        @NotNull UUID membreId,
        LocalDate dateAbandon,
        @Size(max = 500) String motif,
        @PositiveOrZero Integer moisCotises,
        @PositiveOrZero BigDecimal totalCotise,
        @PositiveOrZero BigDecimal totalRecu
) {}
