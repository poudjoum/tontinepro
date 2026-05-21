package com.tontinepro.tontinepro_backend.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClaimerCompteRequest(

        @NotBlank
        String telephone,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 6)
        String motDePasse
) {}