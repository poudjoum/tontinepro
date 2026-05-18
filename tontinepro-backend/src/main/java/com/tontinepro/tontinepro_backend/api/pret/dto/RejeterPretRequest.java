package com.tontinepro.tontinepro_backend.api.pret.dto;

import jakarta.validation.constraints.NotBlank;

public record RejeterPretRequest(

        @NotBlank(message = "Le motif de rejet est obligatoire")
        String motif
) {}
