package com.tontinepro.tontinepro_backend.api.demande.dto;

import jakarta.validation.constraints.NotBlank;

public record RejeterDemandeRequest(@NotBlank String motifRejet) {}
