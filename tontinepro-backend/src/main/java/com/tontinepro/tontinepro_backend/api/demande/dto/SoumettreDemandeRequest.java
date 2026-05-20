package com.tontinepro.tontinepro_backend.api.demande.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SoumettreDemandeRequest(

        @NotBlank
        String nom,

        @NotBlank
        String prenom,

        @NotBlank @Email
        String email,

        String telephone,

        String motivation
) {}
