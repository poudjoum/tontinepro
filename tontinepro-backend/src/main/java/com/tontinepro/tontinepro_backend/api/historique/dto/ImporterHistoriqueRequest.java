package com.tontinepro.tontinepro_backend.api.historique.dto;

import java.math.BigDecimal;
import java.util.List;

public record ImporterHistoriqueRequest(

        /** Solde de départ de la caisse du trésorier (fond antérieur). */
        BigDecimal fondAnterieurSolde,

        /** Montant alloué à l'épargne de chaque membre depuis le fond antérieur. */
        BigDecimal partEpargneParMembre,

        List<TourPasse>                toursPasses,
        List<CotisationHistorique>     cotisations,
        List<AbsenceSanctionHistorique> absencesSanctions,
        List<EpargneComplementaire>    epargnesComplementaires,
        List<PretHistorique>           prets

) {

    public record TourPasse(
            String telephone,
            String date,           // yyyy-MM-dd
            BigDecimal montantRecu
    ) {}

    public record CotisationHistorique(
            String telephone,
            int    mois,
            int    annee,
            BigDecimal montantCotisation,
            BigDecimal montantFondAide,
            String cotisationStatut,   // PAYEE | EN_RETARD | IMPAYEE
            String fondAideStatut      // PAYEE | EN_RETARD | IMPAYEE
    ) {}

    public record AbsenceSanctionHistorique(
            String     telephone,
            String     dateReunion,      // yyyy-MM-dd
            boolean    justifiee,
            BigDecimal sanctionMontant,  // 0 = pas de sanction
            boolean    sanctionPayee
    ) {}

    public record EpargneComplementaire(
            String     telephone,
            BigDecimal montant,
            String     date              // yyyy-MM-dd
    ) {}

    public record PretHistorique(
            String     telephone,
            BigDecimal montant,
            String     datePret,         // yyyy-MM-dd
            int        dureeMois,
            BigDecimal montantRembourse
    ) {}
}