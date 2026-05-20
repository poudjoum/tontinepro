package com.tontinepro.tontinepro_backend.api.profil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangerMotDePasseRequest(
        @NotBlank String ancienMotDePasse,
        @NotBlank @Size(min = 8, message = "Le nouveau mot de passe doit faire au moins 8 caractères") String nouveauMotDePasse
) {}
