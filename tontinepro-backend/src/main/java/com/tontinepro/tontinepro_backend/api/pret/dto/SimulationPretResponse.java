package com.tontinepro.tontinepro_backend.api.pret.dto;

import java.math.BigDecimal;
import java.util.List;

public record SimulationPretResponse(

        BigDecimal montantPrincipal,
        BigDecimal tauxInteretAnnuel,
        int dureeMois,
        BigDecimal mensualite,
        BigDecimal montantTotal,
        BigDecimal totalInterets,
        List<LigneAmortissement> echeances
) {
    public record LigneAmortissement(
            int numero,
            BigDecimal montantCapital,
            BigDecimal montantInteret,
            BigDecimal mensualite,
            BigDecimal soldeRestant
    ) {}
}
