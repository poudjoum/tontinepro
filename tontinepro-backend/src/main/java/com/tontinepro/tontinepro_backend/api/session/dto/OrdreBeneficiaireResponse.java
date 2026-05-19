package com.tontinepro.tontinepro_backend.api.session.dto;

import com.tontinepro.tontinepro_backend.domain.session.OrdreBeneficiaire;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OrdreBeneficiaireResponse(
        UUID id,
        UUID membreId,
        String membreNom,
        String membrePrenom,
        String membreMatricule,
        int ordre,
        LocalDate dateBenefice,
        BigDecimal montantRecu,
        boolean beneficie
) {
    public static OrdreBeneficiaireResponse from(OrdreBeneficiaire ob) {
        return new OrdreBeneficiaireResponse(
                ob.getId(),
                ob.getMembre().getId(),
                ob.getMembre().getNom(),
                ob.getMembre().getPrenom(),
                ob.getMembre().getMatricule(),
                ob.getOrdre(),
                ob.getDateBenefice(),
                ob.getMontantRecu(),
                ob.isBeneficie()
        );
    }
}
