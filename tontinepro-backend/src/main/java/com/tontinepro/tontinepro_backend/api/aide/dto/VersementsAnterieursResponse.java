package com.tontinepro.tontinepro_backend.api.aide.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Fond de caisse déjà versé avant l'application, par membre actif, pour une année. */
public record VersementsAnterieursResponse(
        short annee,
        BigDecimal obligationAnnuelle,
        List<Ligne> lignes,
        BigDecimal total
) {
    public record Ligne(
            UUID membreId,
            String matricule,
            String nomPrenom,
            BigDecimal montant
    ) {}
}
