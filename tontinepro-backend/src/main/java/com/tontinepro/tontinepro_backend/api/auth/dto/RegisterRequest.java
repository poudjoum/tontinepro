package com.tontinepro.tontinepro_backend.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String password,

        String telephone
) {}
