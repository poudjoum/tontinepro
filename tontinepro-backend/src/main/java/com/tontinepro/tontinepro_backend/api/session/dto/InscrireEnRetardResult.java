package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InscrireEnRetardResult(
        UUID membreId,
        String nomMembre,
        String matricule,
        int positionDansSession,
        LocalDate dateBeneficePrevue,
        int toursRattrapes,
        BigDecimal totalRattrapage,
        List<MembreEligibleRetardResponse.DetailTourRattrapage> details,
        SessionResponse sessionMiseAJour
) {}
