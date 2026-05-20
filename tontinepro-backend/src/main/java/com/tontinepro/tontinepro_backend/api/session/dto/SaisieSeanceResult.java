package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;

public record SaisieSeanceResult(
        int totalDemandes,
        int totalEnregistres,
        int totalDejaPayes,
        BigDecimal montantTontineCollecte,
        BigDecimal montantFondAideCollecte
) {}
