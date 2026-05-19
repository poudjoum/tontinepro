package com.tontinepro.tontinepro_backend.api.sanction.dto;

import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SanctionResponse(
        UUID id,
        UUID membreId,
        String membreNom,
        String membrePrenom,
        String membreMatricule,
        UUID tontineId,
        Sanction.TypeSanction typeSanction,
        BigDecimal montant,
        String motif,
        boolean payee,
        UUID referenceId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static SanctionResponse from(Sanction s) {
        return new SanctionResponse(
                s.getId(),
                s.getMembre().getId(),
                s.getMembre().getNom(),
                s.getMembre().getPrenom(),
                s.getMembre().getMatricule(),
                s.getTontine().getId(),
                s.getTypeSanction(),
                s.getMontant(),
                s.getMotif(),
                s.isPayee(),
                s.getReferenceId(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
