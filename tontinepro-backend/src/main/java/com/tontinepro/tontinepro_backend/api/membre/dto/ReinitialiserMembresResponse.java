package com.tontinepro.tontinepro_backend.api.membre.dto;

import java.util.List;

public record ReinitialiserMembresResponse(
        List<MembreInfo> supprimes,
        List<MembreInfo> ignores
) {
    public record MembreInfo(String nom, String prenom, String telephone, String raison) {}
}