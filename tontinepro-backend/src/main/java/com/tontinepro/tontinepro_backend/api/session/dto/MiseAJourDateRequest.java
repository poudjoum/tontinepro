package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MiseAJourDateRequest(
        @NotNull
        LocalDate dateProchaineTontine
) {}
