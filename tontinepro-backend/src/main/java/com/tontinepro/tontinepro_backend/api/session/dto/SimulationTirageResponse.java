package com.tontinepro.tontinepro_backend.api.session.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Résultat d'une simulation de tirage au sort pour une session de tontine classique.
 * Retourne l'ordre aléatoire des bénéficiaires sans persister les données.
 */
public record SimulationTirageResponse(
        UUID tontineId,
        int prochainNumeroSession,
        LocalDate dateDebut,
        LocalDate dateFin,
        int nombreMembres,
        List<OrdreBeneficiaireResponse> beneficiaires,

        /** true si un tirage a déjà été figé pour cette session (numéro) — le tirage ne peut plus être relancé. */
        boolean dejaFige
) {}
