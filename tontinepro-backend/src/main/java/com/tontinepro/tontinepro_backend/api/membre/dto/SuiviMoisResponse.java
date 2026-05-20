package com.tontinepro.tontinepro_backend.api.membre.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SuiviMoisResponse(
        int mois,
        int annee,
        List<CotisationItem> cotisations,
        List<ContributionItem> contributions,
        List<SanctionItem> sanctions,
        List<EcheanceItem> echeancesPret,
        List<MouvementEpargneItem> mouvementsEpargne
) {

    public record CotisationItem(
            UUID id,
            String tontineNom,
            BigDecimal montant,
            String statut,
            OffsetDateTime datePaiement
    ) {}

    public record ContributionItem(
            UUID id,
            String tontineNom,
            BigDecimal montant,
            String statut,
            OffsetDateTime datePaiement
    ) {}

    public record SanctionItem(
            UUID id,
            String tontineNom,
            String typeSanction,
            BigDecimal montant,
            String motif,
            boolean payee,
            OffsetDateTime date
    ) {}

    public record EcheanceItem(
            UUID id,
            UUID pretId,
            String tontineNom,
            int numeroEcheance,
            BigDecimal montantTotal,
            String statut,
            LocalDate dateEcheance,
            OffsetDateTime datePaiement
    ) {}

    public record MouvementEpargneItem(
            UUID id,
            String tontineNom,
            String typeMouvement,
            BigDecimal montant,
            BigDecimal soldeApres,
            OffsetDateTime date
    ) {}
}
