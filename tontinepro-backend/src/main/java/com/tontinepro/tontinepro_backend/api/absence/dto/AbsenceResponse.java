package com.tontinepro.tontinepro_backend.api.absence.dto;

import com.tontinepro.tontinepro_backend.domain.absence.Absence;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AbsenceResponse(
        UUID id,
        UUID membreId,
        String membreNom,
        String membrePrenom,
        String membreMatricule,
        UUID tontineId,
        LocalDate dateReunion,
        boolean justifiee,
        String motif,
        OffsetDateTime createdAt
) {
    public static AbsenceResponse from(Absence a) {
        return new AbsenceResponse(
                a.getId(),
                a.getMembre().getId(),
                a.getMembre().getNom(),
                a.getMembre().getPrenom(),
                a.getMembre().getMatricule(),
                a.getTontine().getId(),
                a.getDateReunion(),
                a.isJustifiee(),
                a.getMotif(),
                a.getCreatedAt()
        );
    }
}
