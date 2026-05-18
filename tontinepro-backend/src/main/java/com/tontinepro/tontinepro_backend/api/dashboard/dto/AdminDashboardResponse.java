package com.tontinepro.tontinepro_backend.api.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminDashboardResponse(

        UUID       tontineId,
        String     tontineNom,

        // Membres
        long membresActifs,
        long membresTotal,

        // Cotisations (mois en cours)
        long       cotisationsPayees,
        long       cotisationsEnRetard,
        long       cotisationsEnAttente,
        BigDecimal montantCollecte,
        BigDecimal montantAttendu,

        // Épargne
        BigDecimal totalEpargne,

        // Fonds d'aide
        BigDecimal fondsAideSolde,

        // Prêts
        long       pretsEnCours,
        BigDecimal encoursPrets,

        // Alertes (actions requises)
        long aidesEnAttente,
        long pretsEnAttente
) {}
