package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Avance versée sur un tour pas encore payé ; montant nul ou absent = supprimer l'avance. */
public record AvanceTourRequest(@PositiveOrZero BigDecimal montant, LocalDate dateAvance) {}
