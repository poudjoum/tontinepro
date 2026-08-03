package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Demande d'aide. Deux formes :
 *  - Barème : `rubriqueId` fourni → type et montant sont déduits de la rubrique.
 *  - Libre  : `typeAide` + `montantDemande` fournis directement.
 * La validation croisée est faite dans le service.
 */
public record DemandeAideRequest(

        UUID rubriqueId,

        Aide.TypeAide typeAide,

        BigDecimal montantDemande,

        @NotBlank(message = "Le motif est obligatoire")
        String motif,

        String justificatifUrl
) {}
