package com.tontinepro.tontinepro_backend.api.superadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SoumettreReinitMdpRequest(

        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        String telephone
) {}
