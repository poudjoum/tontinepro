package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Validation d'un tour. {@code montantRecu} : montant total remis au bénéficiaire (avance
 * comprise), saisi par le bureau ; {@code null} = calculé par l'application.
 */
public record ValiderBeneficeRequest(@PositiveOrZero BigDecimal montantRecu) {}
