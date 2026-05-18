package com.tontinepro.tontinepro_backend.api.dashboard.dto;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;

import java.math.BigDecimal;
import java.util.UUID;

public record MembreDashboardResponse(

        // Profil
        String          matricule,
        String          nom,
        String          prenom,
        Membre.Fonction fonction,
        Membre.Statut   statut,
        String          tontineNom,

        // Épargne
        BigDecimal soldeEpargne,

        // Dernière cotisation connue
        Short       derniereCotisationMois,
        Short       derniereCotisationAnnee,
        BigDecimal  montantCotisation,
        String      statutDerniereCotisation,

        // Prêt actif (null si aucun)
        UUID       pretActifId,
        BigDecimal pretMontantPrincipal,
        BigDecimal pretMontantTotal,
        short      pretDureeMois,
        long       pretEcheancesRestantes,

        // Notifications
        long notificationsNonLues
) {}
