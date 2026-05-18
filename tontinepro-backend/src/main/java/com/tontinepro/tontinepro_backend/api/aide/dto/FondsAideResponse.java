package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.FondsAide;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FondsAideResponse(

        UUID id,
        UUID tontineId,
        String tontineNom,
        Tontine.ModeContributionAide modeContributionAide,
        BigDecimal montantCotisationAide,
        BigDecimal solde,
        OffsetDateTime updatedAt
) {
    public static FondsAideResponse from(FondsAide f) {
        return new FondsAideResponse(
                f.getId(),
                f.getTontine().getId(),
                f.getTontine().getNom(),
                f.getTontine().getModeContributionAide(),
                f.getTontine().getMontantCotisationAide(),
                f.getSolde(),
                f.getUpdatedAt()
        );
    }
}