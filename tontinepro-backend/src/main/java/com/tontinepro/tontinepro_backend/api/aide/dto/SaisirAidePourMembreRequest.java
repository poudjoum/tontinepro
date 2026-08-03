package com.tontinepro.tontinepro_backend.api.aide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Saisie d'une aide par le bureau (secrétaire/admin) au nom d'un membre.
 * L'aide reste à l'état PROPOSEE tant que le membre n'a pas marqué son accord.
 */
public record SaisirAidePourMembreRequest(

        @NotNull(message = "Le membre bénéficiaire est obligatoire")
        UUID membreId,

        @NotNull(message = "La rubrique d'aide est obligatoire")
        UUID rubriqueId,

        /** Variante choisie si la rubrique en propose (ex. « Père »/« Mère »). */
        String variante,

        @NotBlank(message = "Le motif est obligatoire")
        String motif,

        String justificatifUrl
) {}
