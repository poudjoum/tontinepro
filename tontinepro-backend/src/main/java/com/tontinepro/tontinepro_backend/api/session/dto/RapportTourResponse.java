package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RapportTourResponse(

        UUID sessionId,
        int sessionNumero,
        String tontineNom,

        LocalDate dateTour,
        int mois,
        int annee,

        // Bénéficiaire
        UUID beneficiaireId,
        String beneficiaireNom,
        String beneficiaireMatricule,

        // Tableau membres
        List<LigneRapport> lignes,

        // Totaux
        BigDecimal totalCotisations,
        BigDecimal totalFond,
        BigDecimal totalRepas,
        BigDecimal totalSanctions,

        // Bilan bénéficiaire
        BigDecimal potBrut,              // Σcotisations + Σrepas
        BigDecimal fondAideObligation,   // obligation annuelle
        BigDecimal fondAidePayeAnnee,    // déjà payé par le bénéficiaire cette année
        BigDecimal detteFondAide,        // obligation - payé (min 0)
        BigDecimal cagnotteBeneficiaire  // potBrut - detteFondAide

) {
    public record LigneRapport(
            UUID membreId,
            String nomPrenom,
            String matricule,
            BigDecimal cotisation,
            BigDecimal fond,
            BigDecimal repas,
            BigDecimal sanctions,
            boolean paye
    ) {}
}
