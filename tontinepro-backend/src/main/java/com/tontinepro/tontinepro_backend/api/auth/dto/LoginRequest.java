package com.tontinepro.tontinepro_backend.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        /** Identifiant de connexion : email OU numéro de téléphone. */
        @NotBlank
        String email,

        @NotBlank
        String password
) {}
