package com.tontinepro.tontinepro_backend.api.aide.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Rattrapage de fin de session d'un membre : ses parts dues sur ces aides versées. */
public record EncaisserRattrapageRequest(
        @NotNull UUID membreId,
        @NotEmpty List<UUID> aideIds
) {}
