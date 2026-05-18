package com.tontinepro.tontinepro_backend.api.aide.dto;

import jakarta.validation.constraints.NotBlank;

public record RejeterAideRequest(

        @NotBlank(message = "Le motif de rejet est obligatoire")
        String motifRejet
) {}