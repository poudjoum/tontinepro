package com.tontinepro.tontinepro_backend.api.epargne.dto;

import com.tontinepro.tontinepro_backend.domain.epargne.MouvementEpargne;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MouvementEpargneResponse(

        UUID id,
        UUID compteId,
        MouvementEpargne.TypeMouvement typeMouvement,
        BigDecimal montant,
        BigDecimal soldeApres,
        String reference,
        OffsetDateTime createdAt
) {
    public static MouvementEpargneResponse from(MouvementEpargne m) {
        return new MouvementEpargneResponse(
                m.getId(),
                m.getCompte().getId(),
                m.getTypeMouvement(),
                m.getMontant(),
                m.getSoldeApres(),
                m.getReference(),
                m.getCreatedAt()
        );
    }
}
