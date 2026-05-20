package com.tontinepro.tontinepro_backend.api.superadmin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TontinePlatformeResponse(
        UUID id,
        String nom,
        String description,
        boolean actif,
        int nombreMembresActifs,
        String emailPresident,
        String emailSecretaire,
        RedevanceInfo redevance
) {
    public record RedevanceInfo(
            UUID id,
            BigDecimal montant,
            String periodicite,
            String statut,
            LocalDate prochainPaiement
    ) {}
}
