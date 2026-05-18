package com.tontinepro.tontinepro_backend.api.membre.dto;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateMembreRequest(

        @NotNull UUID userId,
        @NotNull UUID tontineId,
        @NotBlank String nom,
        @NotBlank String prenom,
        LocalDate dateAdhesion,

        /** Fonction dans le bureau. Optionnel — défaut : MEMBRE_ORDINAIRE. */
        Membre.Fonction fonction
) {}
