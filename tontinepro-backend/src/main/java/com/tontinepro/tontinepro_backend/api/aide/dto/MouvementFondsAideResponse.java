package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.MouvementFondsAide;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MouvementFondsAideResponse(

        UUID id,
        MouvementFondsAide.TypeMouvement typeMouvement,
        BigDecimal montant,
        BigDecimal soldeApres,
        UUID aideId,
        UUID membreId,
        String membreMatricule,
        String description,
        OffsetDateTime createdAt
) {
    public static MouvementFondsAideResponse from(MouvementFondsAide m) {
        return new MouvementFondsAideResponse(
                m.getId(),
                m.getTypeMouvement(),
                m.getMontant(),
                m.getSoldeApres(),
                m.getAide() != null ? m.getAide().getId() : null,
                m.getMembre() != null ? m.getMembre().getId() : null,
                m.getMembre() != null ? m.getMembre().getMatricule() : null,
                m.getDescription(),
                m.getCreatedAt()
        );
    }
}