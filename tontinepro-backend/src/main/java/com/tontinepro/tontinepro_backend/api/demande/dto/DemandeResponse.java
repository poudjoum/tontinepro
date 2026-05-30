package com.tontinepro.tontinepro_backend.api.demande.dto;

import com.tontinepro.tontinepro_backend.domain.demande.DemandeAdhesion;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DemandeResponse(
        UUID id,
        UUID tontineId,
        String tontineNom,
        String nom,
        String prenom,
        String email,
        String telephone,
        String motivation,
        DemandeAdhesion.Statut statut,
        String motifRejet,
        Membre.TypeParticipation typeParticipation,
        Membre.ModePaiementAide modePaiementAide,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static DemandeResponse from(DemandeAdhesion d) {
        return new DemandeResponse(
                d.getId(),
                d.getTontine().getId(),
                d.getTontine().getNom(),
                d.getNom(),
                d.getPrenom(),
                d.getEmail(),
                d.getTelephone(),
                d.getMotivation(),
                d.getStatut(),
                d.getMotifRejet(),
                d.getTypeParticipation(),
                d.getModePaiementAide(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
