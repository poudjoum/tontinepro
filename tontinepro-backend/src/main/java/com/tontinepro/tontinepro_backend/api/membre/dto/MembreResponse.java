package com.tontinepro.tontinepro_backend.api.membre.dto;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MembreResponse(

        UUID id,
        String matricule,
        String nom,
        String prenom,
        LocalDate dateAdhesion,
        Membre.Statut statut,
        UUID userId,
        String userEmail,
        UUID tontineId,
        String tontineNom,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static MembreResponse from(Membre m) {
        return new MembreResponse(
                m.getId(),
                m.getMatricule(),
                m.getNom(),
                m.getPrenom(),
                m.getDateAdhesion(),
                m.getStatut(),
                m.getUser().getId(),
                m.getUser().getEmail(),
                m.getTontine().getId(),
                m.getTontine().getNom(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
