package com.tontinepro.tontinepro_backend.api.invitation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenererInvitationRequest(

        @NotNull
        UUID tontineId,

        @Min(1)
        Integer dureeHeures
) {}
