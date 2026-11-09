package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Requête de simulation de tirage au sort pour une session de tontine classique.
 * Permet de prévisualiser l'ordre aléatoire des bénéficiaires sans persister.
 */
public record SimulationTirageRequest(

        @NotNull
        UUID tontineId,

        @NotNull
        LocalDate dateDebut,

        /** Objectif de membres à atteindre avant le premier tour (optionnel). */
        Integer cibleMembres
) {}
