package com.tontinepro.tontinepro_backend.api.session.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MettreAJourDateBeneficeRequest(

        @NotNull
        @Future(message = "La date doit être dans le futur")
        LocalDate dateBenefice
) {}
