package com.tontinepro.tontinepro_backend.api.tontine.dto;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.validation.constraints.NotBlank;

public record RejoindreOuvertRequest(

        @NotBlank
        String nom,

        @NotBlank
        String prenom,

        String telephone,

        Membre.TypeParticipation typeParticipation,

        Membre.ModePaiementAide modePaiementAide
) {}
