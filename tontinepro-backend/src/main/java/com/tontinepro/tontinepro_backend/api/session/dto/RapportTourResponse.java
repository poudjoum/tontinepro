package com.tontinepro.tontinepro_backend.api.session.dto;

import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;

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

        // Tableau membres TONTINE
        List<LigneRapport> lignes,

        // Totaux
        BigDecimal totalCotisations,
        BigDecimal totalFond,
        BigDecimal totalRepas,
        BigDecimal totalSanctions,

        // Bilan bénéficiaire
        BigDecimal potBrut,              // Σcotisations + Σrepas
        BigDecimal fondBeneficiaire,     // fond d'aide versé par le bénéficiaire ce tour (déduit du pot)
        BigDecimal cagnotteBeneficiaire, // potBrut - fondBeneficiaire

        // Contributeurs Aide Sociale (section bas de rapport)
        List<ContributeurAideSociale> contributeursSolidaires

) {
    public record LigneRapport(
            UUID membreId,
            String nomPrenom,
            String matricule,
            BigDecimal cotisation,
            BigDecimal fond,
            BigDecimal repas,
            BigDecimal sanctions,
            boolean paye,
            Cotisation.MoyenPaiement moyenPaiement
    ) {}

    public record ContributeurAideSociale(
            UUID membreId,
            String nomPrenom,
            String matricule,
            Membre.ModePaiementAide modePaiement,
            BigDecimal montantFond,
            boolean paye
    ) {}
}
