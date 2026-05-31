package com.tontinepro.tontinepro_backend.api.superadmin.dto;

import com.tontinepro.tontinepro_backend.domain.user.DemandeReinitMdp;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DemandeReinitMdpResponse(
        UUID id,
        String email,
        String nom,
        String prenom,
        String telephone,
        DemandeReinitMdp.Statut statut,
        OffsetDateTime createdAt
) {
    public static DemandeReinitMdpResponse from(DemandeReinitMdp d) {
        return new DemandeReinitMdpResponse(
                d.getId(), d.getEmail(), d.getNom(), d.getPrenom(),
                d.getTelephone(), d.getStatut(), d.getCreatedAt());
    }
}
