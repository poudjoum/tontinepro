package com.tontinepro.tontinepro_backend.api.aide.dto;

import com.tontinepro.tontinepro_backend.domain.aide.ArriereMembre;
import com.tontinepro.tontinepro_backend.domain.aide.ReglementArriere;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Arriéré d'un membre, avec ses règlements et le reste dû. */
public record ArriereResponse(
        UUID id,
        UUID membreId,
        String matricule,
        String nom,
        String prenom,
        String libelle,
        BigDecimal montant,
        LocalDate dateConstat,
        BigDecimal regle,
        BigDecimal resteDu,
        List<Reglement> reglements
) {
    public record Reglement(UUID id, BigDecimal montant, LocalDate dateReglement) {}

    public static ArriereResponse from(ArriereMembre a, List<ReglementArriere> reglements) {
        BigDecimal regle = reglements.stream().map(ReglementArriere::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ArriereResponse(a.getId(), a.getMembre().getId(), a.getMembre().getMatricule(),
                a.getMembre().getNom(), a.getMembre().getPrenom(), a.getLibelle(), a.getMontant(),
                a.getDateConstat(), regle, a.getMontant().subtract(regle),
                reglements.stream().map(r -> new Reglement(r.getId(), r.getMontant(), r.getDateReglement()))
                        .toList());
    }
}
