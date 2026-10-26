package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Abandon d'un membre en cours de session à lot (date du jour si absente). */
public record DeclarerAbandonRequest(
        @NotNull UUID membreId,
        LocalDate dateAbandon,
        @Size(max = 500) String motif
) {}
