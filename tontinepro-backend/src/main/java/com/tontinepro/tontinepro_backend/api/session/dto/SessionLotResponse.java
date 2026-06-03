package com.tontinepro.tontinepro_backend.api.session.dto;

import com.tontinepro.tontinepro_backend.domain.session.OrdreBeneficiaire;
import com.tontinepro.tontinepro_backend.domain.session.ParticipationLot;
import com.tontinepro.tontinepro_backend.domain.session.PartLot;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Vue d'une session « à lot » : adhésions (avant figeage) et lots/tours (après figeage). */
public record SessionLotResponse(
        UUID sessionId,
        boolean figee,
        LocalDate dateFigeage,
        BigDecimal montantLot,
        BigDecimal cagnotte,
        Integer nombreTours,
        BigDecimal tresorerieLots,
        List<Adhesion> adhesions,
        List<Lot> lots
) {
    public record Adhesion(UUID membreId, String nom, String prenom, String matricule, BigDecimal montantMensuel) {}

    public record PartMembre(UUID membreId, String nom, String prenom, BigDecimal montantMensuel, BigDecimal partCagnotte) {}

    public record Lot(UUID ordreBeneficiaireId, int ordre, boolean beneficie,
                      BigDecimal montantRecu, List<PartMembre> membres) {}

    public static SessionLotResponse build(UUID sessionId, boolean figee, LocalDate dateFigeage,
                                           BigDecimal montantLot, BigDecimal cagnotte, Integer nombreTours,
                                           BigDecimal tresorerieLots,
                                           List<ParticipationLot> participations,
                                           List<OrdreBeneficiaire> ordres,
                                           List<PartLot> parts) {

        List<Adhesion> adhesions = participations.stream()
                .map(p -> new Adhesion(p.getMembre().getId(), p.getMembre().getNom(),
                        p.getMembre().getPrenom(), p.getMembre().getMatricule(), p.getMontantMensuel()))
                .toList();

        Map<UUID, List<PartLot>> parParTour = parts.stream()
                .collect(Collectors.groupingBy(pl -> pl.getOrdreBeneficiaire().getId()));

        List<Lot> lots = ordres.stream()
                .map(ob -> new Lot(
                        ob.getId(), ob.getOrdre(), ob.isBeneficie(), ob.getMontantRecu(),
                        parParTour.getOrDefault(ob.getId(), List.of()).stream()
                                .map(pl -> new PartMembre(pl.getMembre().getId(), pl.getMembre().getNom(),
                                        pl.getMembre().getPrenom(), pl.getMontantMensuel(), pl.getPartCagnotte()))
                                .toList()))
                .toList();

        return new SessionLotResponse(sessionId, figee, dateFigeage, montantLot, cagnotte,
                nombreTours, tresorerieLots, adhesions, lots);
    }
}
