package com.tontinepro.tontinepro_backend.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmerTwoFaRequest(

        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Le code TOTP doit être composé de 6 chiffres")
        String code
) {}
