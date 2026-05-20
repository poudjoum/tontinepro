package com.tontinepro.tontinepro_backend.api.tontine.dto;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreerTontineAvecCompteRequest(

        // ── Compte ──────────────────────────────────────────────────────────
        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8)
        String password,

        @NotBlank
        String nom,

        @NotBlank
        String prenom,

        String telephone,

        // ── Tontine ─────────────────────────────────────────────────────────
        @NotBlank
        String tontineNom,

        String tontineDescription,

        @NotNull @DecimalMin("0.01")
        BigDecimal montantCotisationMin,

        Tontine.TypeAcces typeAcces,

        String descriptionAcces
) {}
