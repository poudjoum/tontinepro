package com.tontinepro.tontinepro_backend.api.session.dto;

import com.tontinepro.tontinepro_backend.domain.session.SessionTontine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        UUID tontineId,
        int numero,
        LocalDate dateDebut,
        LocalDate dateFin,
        LocalDate dateProchaineTontine,
        SessionTontine.Statut statut,
        int nombreMembres,
        Integer cibleMembres,
        BigDecimal potReserve,
        List<OrdreBeneficiaireResponse> beneficiaires
) {
    public static SessionResponse from(SessionTontine s, List<OrdreBeneficiaireResponse> beneficiaires) {
        return new SessionResponse(
                s.getId(),
                s.getTontine().getId(),
                s.getNumero(),
                s.getDateDebut(),
                s.getDateFin(),
                s.getDateProchaineTontine(),
                s.getStatut(),
                s.getNombreMembres(),
                s.getCibleMembres(),
                s.getPotReserve(),
                beneficiaires
        );
    }
}
