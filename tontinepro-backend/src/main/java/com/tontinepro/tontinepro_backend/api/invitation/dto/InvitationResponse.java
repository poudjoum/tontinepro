package com.tontinepro.tontinepro_backend.api.invitation.dto;

import com.tontinepro.tontinepro_backend.domain.invitation.InvitationToken;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID tontineId,
        String tontineNom,
        String token,
        boolean utilise,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {
    public static InvitationResponse from(InvitationToken t) {
        return new InvitationResponse(
                t.getId(),
                t.getTontine().getId(),
                t.getTontine().getNom(),
                t.getToken(),
                t.isUtilise(),
                t.getExpiresAt(),
                t.getCreatedAt()
        );
    }
}
