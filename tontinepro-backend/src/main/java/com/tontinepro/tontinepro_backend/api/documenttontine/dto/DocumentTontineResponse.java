package com.tontinepro.tontinepro_backend.api.documenttontine.dto;

import com.tontinepro.tontinepro_backend.domain.documenttontine.DocumentTontine;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentTontineResponse(
        UUID id,
        UUID tontineId,
        DocumentTontine.TypeDocument typeDocument,
        String nomFichier,
        long tailleOctets,
        String contentType,
        OffsetDateTime createdAt
) {
    public static DocumentTontineResponse from(DocumentTontine d) {
        return new DocumentTontineResponse(
                d.getId(), d.getTontine().getId(),
                d.getTypeDocument(), d.getNomFichier(),
                d.getTailleOctets(), d.getContentType(), d.getCreatedAt());
    }
}
