package com.tontinepro.tontinepro_backend.api.absence.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record AppelPresenceRequest(
        @NotNull UUID tontineId,
        @NotNull LocalDate dateReunion,
        @NotNull Set<UUID> membresPresentsIds
) {}
