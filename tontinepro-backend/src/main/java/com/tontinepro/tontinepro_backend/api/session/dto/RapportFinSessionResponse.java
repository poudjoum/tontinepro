package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Rapport de fin de session : bilan financier complet généré à la clôture
 * (ou à tout moment) d'une session. Agrège les cotisations, repas et fonds
 * collectés sur toute la durée de la session, les montants redistribués aux
 * bénéficiaires, l'état de l'épargne, des prêts et du fonds de solidarité,
 * plus une fiche individuelle par membre.
 */
public record RapportFinSessionResponse(

        UUID sessionId,
        int sessionNumero,
        String tontineNom,
        LocalDate dateDebut,
        LocalDate dateFin,
        String statut,
        int nombreMembres,
        int nbToursRealises,
        int nbToursTotal,

        // ── Récapitulatif global ────────────────────────────────────────────
        BigDecimal totalCotisations,        // Σ part tontine collectée (PAYEE) sur la période
        BigDecimal totalRepas,              // Σ repas collectés
        BigDecimal totalFondAide,           // Σ fonds d'aide collecté
        BigDecimal totalRedistribue,        // Σ montants reçus par les bénéficiaires
        BigDecimal soldeFondsSolidarite,    // solde du fonds de solidarité (snapshot à la clôture)
        BigDecimal totalEpargne,            // Σ soldes des comptes épargne (snapshot)
        BigDecimal pretsTotalDecaisse,      // Σ principal des prêts décaissés
        BigDecimal pretsInteretsGeneres,    // Σ (total − principal) des prêts soldés
        BigDecimal pretsEnCours,            // Σ montant restant des prêts en cours

        // ── Fiche individuelle par membre ───────────────────────────────────
        List<FicheMembre> fiches
) {
    public record FicheMembre(
            UUID membreId,
            String matricule,
            String nomPrenom,
            BigDecimal cotise,
            BigDecimal fondAideVerse,
            BigDecimal repasVerse,
            boolean aBeneficie,
            LocalDate dateBenefice,
            BigDecimal recu,
            BigDecimal epargne,
            BigDecimal pretEnCours
    ) {}
}
