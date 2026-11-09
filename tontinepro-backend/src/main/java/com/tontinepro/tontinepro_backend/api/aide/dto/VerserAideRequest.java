package com.tontinepro.tontinepro_backend.api.aide.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Versement d'une aide au bénéficiaire avec détail des moyens de paiement.
 * La somme des trois montants (cash + OM + MoMo) doit être égale au montant accordé.
 */
public record VerserAideRequest(

        /** Montant remis en espèces au bénéficiaire. */
        @PositiveOrZero(message = "Le montant cash doit être positif ou nul")
        BigDecimal montantCash,

        /** Montant versé par Orange Money au bénéficiaire. */
        @PositiveOrZero(message = "Le montant Orange Money doit être positif ou nul")
        BigDecimal montantOm,

        /** Montant versé par MTN MoMo au bénéficiaire. */
        @PositiveOrZero(message = "Le montant MTN MoMo doit être positif ou nul")
        BigDecimal montantMomo,

        /** Référence de la transaction Orange Money. */
        String refOm,

        /** Référence de la transaction MTN MoMo. */
        String refMomo
) {}
