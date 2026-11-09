package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Validation d'un tour. {@code montantRecu} : montant total remis au bénéficiaire (avance
 * comprise), saisi par le bureau ; {@code null} = calculé par l'application.
 * <p>Les moyens de paiement (cash, OM, MoMo) permettent de détailler comment le montant
 * a été versé, pour faciliter la vérification de caisse. La somme des trois montants
 * doit être égale à {@code montantRecu}.</p>
 */
public record ValiderBeneficeRequest(
        @PositiveOrZero BigDecimal montantRecu,

        /** Montant remis en espèces au bénéficiaire. */
        @PositiveOrZero BigDecimal montantCash,

        /** Montant versé par Orange Money au bénéficiaire. */
        @PositiveOrZero BigDecimal montantOm,

        /** Montant versé par MTN MoMo au bénéficiaire. */
        @PositiveOrZero BigDecimal montantMomo,

        /** Référence de la transaction Orange Money. */
        String refOm,

        /** Référence de la transaction MTN MoMo. */
        String refMomo
) {}
