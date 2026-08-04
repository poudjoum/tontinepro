package com.tontinepro.tontinepro_backend.api.aide.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Compte rendu de la suppression d'une aide : ce qui a été défait sur la
 * trésorerie, pour que le bureau puisse vérifier que le solde est cohérent.
 */
public record SuppressionAideResponse(

        UUID aideId,
        String libelle,
        String beneficiaireNom,

        int nbContributionsSupprimees,
        BigDecimal montantCollecteAnnule,   // parts déjà versées, retirées du fonds
        BigDecimal montantDecaisseRendu,    // préfinancement/versement, rendu au fonds
        BigDecimal soldeFondsApres          // null si le fonds n'a pas été touché

) {}
