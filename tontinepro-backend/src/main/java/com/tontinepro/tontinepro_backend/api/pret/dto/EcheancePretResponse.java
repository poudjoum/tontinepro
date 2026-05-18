package com.tontinepro.tontinepro_backend.api.pret.dto;

import com.tontinepro.tontinepro_backend.domain.pret.EcheancePret;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EcheancePretResponse(

        UUID id,
        UUID pretId,
        short numeroEcheance,
        LocalDate dateEcheance,
        BigDecimal montantCapital,
        BigDecimal montantInteret,
        BigDecimal montantTotal,
        EcheancePret.Statut statut,
        OffsetDateTime datePaiement
) {
    public static EcheancePretResponse from(EcheancePret e) {
        return new EcheancePretResponse(
                e.getId(),
                e.getPret().getId(),
                e.getNumeroEcheance(),
                e.getDateEcheance(),
                e.getMontantCapital(),
                e.getMontantInteret(),
                e.getMontantTotal(),
                e.getStatut(),
                e.getDatePaiement()
        );
    }
}
