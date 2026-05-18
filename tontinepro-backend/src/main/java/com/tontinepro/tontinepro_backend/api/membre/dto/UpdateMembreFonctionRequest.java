package com.tontinepro.tontinepro_backend.api.membre.dto;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.validation.constraints.NotNull;

public record UpdateMembreFonctionRequest(

        @NotNull(message = "La fonction est obligatoire")
        Membre.Fonction fonction
) {}
