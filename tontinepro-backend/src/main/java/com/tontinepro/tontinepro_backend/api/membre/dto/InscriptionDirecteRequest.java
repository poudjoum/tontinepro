package com.tontinepro.tontinepro_backend.api.membre.dto;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InscriptionDirecteRequest(

        @NotNull
        UUID tontineId,

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8)
        String password,

        @NotBlank
        String nom,

        @NotBlank
        String prenom,

        String telephone,

        Membre.Fonction fonction,

        Membre.TypeParticipation typeParticipation,

        Membre.ModePaiementAide modePaiementAide
) {}
