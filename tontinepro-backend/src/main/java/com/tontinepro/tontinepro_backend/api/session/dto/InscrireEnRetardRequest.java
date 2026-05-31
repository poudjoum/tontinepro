package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;

public record InscrireEnRetardRequest(
        BigDecimal montantCotisationParTour,
        BigDecimal montantRepasParTour,
        BigDecimal montantFondAideParTour
) {}
