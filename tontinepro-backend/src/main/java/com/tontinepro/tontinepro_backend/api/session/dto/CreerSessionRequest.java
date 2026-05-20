package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreerSessionRequest(

        @NotNull
        UUID tontineId,

        @NotNull
        LocalDate dateDebut,

        /** Objectif de membres à atteindre avant le premier tour (optionnel). */
        @Min(1)
        Integer cibleMembres,

        /** Ordre des membres souhaité ; si absent, l'ordre est aléatoire. */
        List<UUID> ordreMembreIds
) {}
