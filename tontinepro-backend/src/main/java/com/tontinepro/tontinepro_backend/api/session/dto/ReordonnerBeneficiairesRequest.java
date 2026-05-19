package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReordonnerBeneficiairesRequest(
        @NotEmpty
        List<UUID> ordreMembreIds
) {}
