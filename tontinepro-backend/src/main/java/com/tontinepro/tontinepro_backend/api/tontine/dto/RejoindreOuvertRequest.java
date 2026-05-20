package com.tontinepro.tontinepro_backend.api.tontine.dto;

import jakarta.validation.constraints.NotBlank;

public record RejoindreOuvertRequest(

        @NotBlank
        String nom,

        @NotBlank
        String prenom,

        String telephone
) {}
