package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SessionBilanResponse(

        UUID sessionId,
        int sessionNumero,

        // Collecte du jour
        int nbMembresPayes,
        BigDecimal potTontineBrut,      // Σ montantTontine des cotisations payées
        BigDecimal fondsAideCollecte,   // Σ montantFondAide — remis au trésorier

        // Bénéficiaire
        UUID beneficiaireId,
        String beneficiaireNom,
        String beneficiairePrenom,
        String beneficiaireMatricule,

        // Calcul de la déduction fonds d'aide
        BigDecimal fondAideAnnuelObligation,   // montantFondAideAnnuelMembre
        BigDecimal fondAidePayeCetteAnnee,     // cumul fonds d'aide payé par le bénéficiaire depuis jan
        BigDecimal dettesFondsAide,            // obligation - payé (si > 0)

        // Montant net remis au bénéficiaire
        BigDecimal potBeneficiaireNet,         // potTontineBrut - dettesFondsAide

        // Détail par membre
        List<LignePaiementMembre> lignes
) {
    public record LignePaiementMembre(
            UUID membreId,
            String matricule,
            String nomPrenom,
            BigDecimal montantTontine,
            BigDecimal montantFondAide,
            BigDecimal total,
            boolean paye
    ) {}
}
