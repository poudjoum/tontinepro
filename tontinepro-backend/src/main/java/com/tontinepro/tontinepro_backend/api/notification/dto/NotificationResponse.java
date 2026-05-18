package com.tontinepro.tontinepro_backend.api.notification.dto;

import com.tontinepro.tontinepro_backend.domain.notification.Notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(

        UUID id,
        Notification.Type type,
        String titre,
        String message,
        boolean lu,
        UUID referenceId,
        String referenceType,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitre(),
                n.getMessage(),
                n.isLu(),
                n.getReferenceId(),
                n.getReferenceType(),
                n.getCreatedAt()
        );
    }
}
