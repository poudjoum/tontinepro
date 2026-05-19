package com.tontinepro.tontinepro_backend.api.absence.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.UUID;

public record EnregistrerAbsenceRequest(

        @NotNull
        UUID membreId,

        @NotNull
        @PastOrPresent
        LocalDate dateReunion,

        boolean justifiee,

        String motif
) {}
