package com.tontinepro.tontinepro_backend.api.aide.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Tableau de collecte des aides d'une tontine, sous forme de matrice
 * Membres (lignes) × Aides actives (colonnes). Chaque cellule = la part due par
 * un membre pour une aide, avec son statut de paiement. L'objectif d'une colonne
 * est le montant total de l'aide.
 */
public record CollecteAidesResponse(

        List<MembreRow> membres,     // lignes (ordre stable)
        List<AideColonne> aides,     // colonnes
        BigDecimal totalObjectif,
        BigDecimal totalCollecte

) {
    public record MembreRow(UUID membreId, String nomPrenom, String matricule) {}

    public record AideColonne(
            UUID aideId,
            String libelle,
            String variante,
            UUID beneficiaireId,
            String beneficiaireNom,
            String statut,
            boolean prefinance,
            BigDecimal objectif,     // montant total de l'aide
            BigDecimal collecte,     // Σ parts payées
            LocalDate dateEcheanceRecouvrement, // date limite de collecte (3 séances)
            boolean enRetard,        // échéance dépassée (des parts restent dues)
            List<Cellule> cellules   // une par ligne membre, même ordre que `membres`
    ) {}

    public record Cellule(
            UUID membreId,
            UUID contributionId,     // null si le membre n'a pas de part pour cette aide
            BigDecimal montant,
            boolean paye,
            boolean estBeneficiaire
    ) {}
}
