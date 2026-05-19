package com.tontinepro.tontinepro_backend.api.invitation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejoindreRequest(

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8)
        String password,

        @NotBlank
        String nom,

        @NotBlank
        String prenom,

        String telephone
) {}
