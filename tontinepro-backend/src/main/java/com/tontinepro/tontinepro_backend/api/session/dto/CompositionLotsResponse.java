package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Aperçu de la composition des lots avant figeage : lots exacts proposés et
 * reliquats qu'aucune combinaison n'absorbe (ils bloquent le figeage).
 */
public record CompositionLotsResponse(
        BigDecimal montantLot,
        int nombreTours,
        BigDecimal cagnotte,
        boolean complete,
        List<LotPropose> lots,
        List<PartProposee> restes
) {
    public record LotPropose(List<PartProposee> parts) {}

    public record PartProposee(UUID membreId, String nom, String prenom, BigDecimal montant,
                               BigDecimal partCagnotte) {}
}
