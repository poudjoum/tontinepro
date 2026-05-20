package com.tontinepro.tontinepro_backend.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères") String nouveauMotDePasse
) {}
