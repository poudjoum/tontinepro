package com.tontinepro.tontinepro_backend.api.tontine.dto;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TontineResponse(

        UUID id,
        String nom,
        String description,
        BigDecimal montantCotisation,
        short jourCotisation,
        BigDecimal tauxInteretPret,
        BigDecimal tauxInteretEpargne,
        boolean actif,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TontineResponse from(Tontine t) {
        return new TontineResponse(
                t.getId(),
                t.getNom(),
                t.getDescription(),
                t.getMontantCotisation(),
                t.getJourCotisation(),
                t.getTauxInteretPret(),
                t.getTauxInteretEpargne(),
                t.isActif(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
