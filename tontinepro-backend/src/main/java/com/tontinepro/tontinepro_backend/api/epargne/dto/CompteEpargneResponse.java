package com.tontinepro.tontinepro_backend.api.epargne.dto;

import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CompteEpargneResponse(

        UUID id,
        UUID membreId,
        String membreMatricule,
        String membreNom,
        String membrePrenom,
        UUID tontineId,
        String tontineNom,
        BigDecimal solde,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CompteEpargneResponse from(CompteEpargne c) {
        return new CompteEpargneResponse(
                c.getId(),
                c.getMembre().getId(),
                c.getMembre().getMatricule(),
                c.getMembre().getNom(),
                c.getMembre().getPrenom(),
                c.getMembre().getTontine().getId(),
                c.getMembre().getTontine().getNom(),
                c.getSolde(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
