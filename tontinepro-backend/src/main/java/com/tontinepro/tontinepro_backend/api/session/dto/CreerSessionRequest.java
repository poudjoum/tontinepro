package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreerSessionRequest(

        @NotNull
        UUID tontineId,

        @NotNull
        LocalDate dateDebut,

        /** Ordre des membres souhaité ; si absent, l'ordre est aléatoire. */
        List<UUID> ordreMembreIds
) {}
