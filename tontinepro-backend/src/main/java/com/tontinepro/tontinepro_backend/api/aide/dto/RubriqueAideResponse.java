package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RubriqueAideResponse(

        UUID id,
        UUID tontineId,
        String libelle,
        Aide.TypeAide typeAide,
        RubriqueAide.ModeCalcul modeCalcul,
        BigDecimal montantReference,
        boolean prefinancable,
        boolean actif,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static RubriqueAideResponse from(RubriqueAide r) {
        return new RubriqueAideResponse(
                r.getId(),
                r.getTontine().getId(),
                r.getLibelle(),
                r.getTypeAide(),
                r.getModeCalcul(),
                r.getMontantReference(),
                r.isPrefinancable(),
                r.isActif(),
                r.getDescription(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
