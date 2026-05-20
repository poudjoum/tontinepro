package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record MonBeneficeResponse(
        UUID sessionId,
        int sessionNumero,
        String tontineNom,
        int ordre,
        int totalMembres,
        LocalDate dateBenefice,
        BigDecimal montantRecu
) {}
