package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Mise à jour de la date de passage d'un bénéficiaire.
 * La date peut être passée : un admin doit pouvoir backdater un tour
 * (reprise d'une tontine démarrée hors application, correction du calendrier).
 */
public record MettreAJourDateBeneficeRequest(

        @NotNull
        LocalDate dateBenefice
) {}
