package com.tontinepro.tontinepro_backend.api.session.dto;

import java.time.LocalDate;
import java.util.UUID;

public record MonTourResponse(
        UUID sessionId,
        int sessionNumero,
        int ordre,
        int totalMembres,
        LocalDate dateBenefice,
        boolean beneficie,
        String tontineNom
) {}
