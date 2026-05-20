package com.tontinepro.tontinepro_backend.api.profil.dto;

public record UpdateProfilRequest(
        String nom,
        String prenom,
        String telephone
) {}
