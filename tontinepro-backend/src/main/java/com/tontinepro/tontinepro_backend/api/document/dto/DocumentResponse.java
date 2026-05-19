package com.tontinepro.tontinepro_backend.api.document.dto;

import com.tontinepro.tontinepro_backend.domain.document.Document;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID membreId,
        String membreNom,
        String membrePrenom,
        String membreMatricule,
        Document.TypeDocument typeDocument,
        String nomFichier,
        long tailleOctets,
        String contentType,
        OffsetDateTime createdAt
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getMembre().getId(),
                d.getMembre().getNom(),
                d.getMembre().getPrenom(),
                d.getMembre().getMatricule(),
                d.getTypeDocument(),
                d.getNomFichier(),
                d.getTailleOctets(),
                d.getContentType(),
                d.getCreatedAt()
        );
    }
}
