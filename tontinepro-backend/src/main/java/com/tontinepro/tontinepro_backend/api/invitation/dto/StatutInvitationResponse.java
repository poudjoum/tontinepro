package com.tontinepro.tontinepro_backend.api.invitation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StatutInvitationResponse(
        boolean valide,
        UUID tontineId,
        String tontineNom,
        String tontineDescription,
        OffsetDateTime expiresAt
) {}
