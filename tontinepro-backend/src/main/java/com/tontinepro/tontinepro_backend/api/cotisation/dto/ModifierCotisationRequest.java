package com.tontinepro.tontinepro_backend.api.cotisation.dto;

import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ModifierCotisationRequest(
        BigDecimal montant,
        BigDecimal montantFondAide,
        BigDecimal montantRepas,
        String referencePaiement,
        Cotisation.Statut statut,
        OffsetDateTime datePaiement,
        Cotisation.MoyenPaiement moyenPaiement
) {}
