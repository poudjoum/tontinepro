package com.tontinepro.tontinepro_backend.api.session.dto;

import com.tontinepro.tontinepro_backend.domain.session.AbandonLot;
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
        List<Lot> lots,
        List<Abandon> abandons,
        SuiviTresorerie tresorerie
) {
    public record Adhesion(UUID membreId, String nom, String prenom, String matricule,
                           BigDecimal montantMensuel, boolean abandonnee) {}

    /** Abandon d'un membre : solde = cotisé − reçu (positif = avoir gelé, négatif = dette). */
    public record Abandon(UUID abandonId, UUID membreId, String nom, String prenom, LocalDate dateAbandon,
                          String motif, int moisCotises, BigDecimal totalCotise, BigDecimal totalRecu,
                          BigDecimal solde, AbandonLot.Statut statut, LocalDate dateRestitution,
                          BigDecimal montantRembourse, BigDecimal resteARembourser) {}

    /**
     * Trésorerie de la session. {@code solde} = encaissé − décaissé ; négatif, c'est ce
     * que la trésorerie avance en attendant que les tours des membres partis arrivent.
     */
    public record SuiviTresorerie(BigDecimal misesEncaissees, BigDecimal remboursementsDettes,
                                  BigDecimal partsVersees, BigDecimal avoirsRestitues, BigDecimal solde,
                                  BigDecimal avoirsGelesARestituer, BigDecimal dettesARecouvrer) {}

    /**
     * Part d'un co-bénéficiaire. Avant versement, {@code montantRetenue} et
     * {@code montantVerse} sont les montants <i>prévus</i> ; après, ceux enregistrés.
     */
    public record PartMembre(UUID partId, UUID membreId, String nom, String prenom,
                             BigDecimal montantMensuel, BigDecimal partCagnotte,
                             boolean verse, boolean annulee, LocalDate dateVersement,
                             BigDecimal montantRetenue, BigDecimal montantVerse) {}

    public record Lot(UUID ordreBeneficiaireId, int ordre, LocalDate dateTour, boolean beneficie,
                      BigDecimal montantRecu, int nombreVerses, List<PartMembre> membres) {}

    public static SessionLotResponse build(UUID sessionId, boolean figee, LocalDate dateFigeage,
                                           BigDecimal montantLot, BigDecimal cagnotte, Integer nombreTours,
                                           BigDecimal tresorerieLots,
                                           List<ParticipationLot> participations,
                                           List<OrdreBeneficiaire> ordres,
                                           List<PartLot> parts,
                                           Map<UUID, LocalDate> datesTour,
                                           Map<UUID, BigDecimal> retenuesPrevues,
                                           List<AbandonLot> abandons,
                                           SuiviTresorerie tresorerie) {

        List<Adhesion> adhesions = participations.stream()
                .map(p -> new Adhesion(p.getMembre().getId(), p.getMembre().getNom(),
                        p.getMembre().getPrenom(), p.getMembre().getMatricule(), p.getMontantMensuel(),
                        p.isAbandonnee()))
                .toList();

        Map<UUID, List<PartLot>> parParTour = parts.stream()
                .collect(Collectors.groupingBy(pl -> pl.getOrdreBeneficiaire().getId()));

        List<Lot> lots = ordres.stream()
                .map(ob -> {
                    List<PartLot> duTour = parParTour.getOrDefault(ob.getId(), List.of());
                    List<PartMembre> membres = duTour.stream().map(pl -> partMembre(pl, retenuesPrevues)).toList();
                    int verses = (int) duTour.stream().filter(PartLot::isVerse).count();
                    return new Lot(ob.getId(), ob.getOrdre(), datesTour.get(ob.getId()), ob.isBeneficie(),
                            ob.getMontantRecu(), verses, membres);
                })
                .toList();

        List<Abandon> vueAbandons = abandons.stream()
                .map(a -> new Abandon(a.getId(), a.getMembre().getId(), a.getMembre().getNom(),
                        a.getMembre().getPrenom(), a.getDateAbandon(), a.getMotif(), a.getMoisCotises(),
                        a.getTotalCotise(), a.getTotalRecu(), a.getSolde(), a.getStatut(),
                        a.getDateRestitution(), a.getMontantRembourse(), a.resteARembourser()))
                .toList();

        return new SessionLotResponse(sessionId, figee, dateFigeage, montantLot, cagnotte,
                nombreTours, tresorerieLots, adhesions, lots, vueAbandons, tresorerie);
    }

    private static PartMembre partMembre(PartLot pl, Map<UUID, BigDecimal> retenuesPrevues) {
        BigDecimal retenue = pl.isVerse()
                ? pl.getMontantRetenue()
                : pl.isAnnulee() ? null : retenuesPrevues.getOrDefault(pl.getId(), BigDecimal.ZERO);
        BigDecimal verse = pl.isVerse()
                ? pl.getMontantVerse()
                : (pl.getPartCagnotte() != null && retenue != null ? pl.getPartCagnotte().subtract(retenue) : null);
        return new PartMembre(pl.getId(), pl.getMembre().getId(), pl.getMembre().getNom(),
                pl.getMembre().getPrenom(), pl.getMontantMensuel(), pl.getPartCagnotte(),
                pl.isVerse(), pl.isAnnulee(), pl.getDateVersement(), retenue, verse);
    }
}
