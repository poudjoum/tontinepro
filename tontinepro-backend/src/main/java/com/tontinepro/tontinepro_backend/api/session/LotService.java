package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.session.dto.CompositionLotsResponse;
import com.tontinepro.tontinepro_backend.api.session.dto.FigerLotRequest;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.session.*;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Logique du mode « tontine à lot » : inscription des mises, et figeage
 * (constitution des lots/tours en combinaisons exactes, cagnotte, ordre tiré au hasard).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LotService {

    private final SessionTontineRepository sessionRepository;
    private final ParticipationLotRepository participationLotRepository;
    private final OrdreBeneficiaireRepository ordreBeneficiaireRepository;
    private final PartLotRepository partLotRepository;
    private final MembreRepository membreRepository;
    private final RetenueFondCaisse retenueFondCaisse;
    private final PeriodiciteService periodiciteService;

    /** Inscrit (ou met à jour) la mise mensuelle d'un membre dans une session à lot. */
    @Transactional
    public ParticipationLot adherer(UUID sessionId, UUID membreId, BigDecimal montantMensuel) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        exigerModeLot(session);
        if (session.isFigee()) {
            throw new IllegalStateException("Session figée : les adhésions sont closes");
        }
        if (montantMensuel == null || montantMensuel.signum() <= 0) {
            throw new IllegalArgumentException("La mise mensuelle doit être strictement positive");
        }
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + membreId));
        if (!membre.getTontine().getId().equals(session.getTontine().getId())) {
            throw new IllegalArgumentException("Ce membre n'appartient pas à cette tontine");
        }

        ParticipationLot p = participationLotRepository
                .findBySessionIdAndMembreId(sessionId, membreId)
                .orElseGet(() -> ParticipationLot.builder().session(session).membre(membre).build());
        p.setMontantMensuel(montantMensuel);
        return participationLotRepository.save(p);
    }

    /**
     * Aperçu de la composition automatique (rien n'est enregistré) : lots exacts et
     * reliquats non placés. Le bureau s'en sert comme base, l'ajuste puis fige.
     */
    @Transactional(readOnly = true)
    public CompositionLotsResponse apercu(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        exigerModeLot(session);
        BigDecimal lot = montantLot(session);
        List<ParticipationLot> participations = participationLotRepository.findAllBySessionId(sessionId);
        Map<UUID, Membre> membres = membresParId(participations);

        CompositionLots.Proposition p = CompositionLots.proposer(mises(participations), lot, new Random());
        int nbTours = p.lots().size();
        List<CompositionLotsResponse.LotPropose> lots = p.lots().stream()
                .map(parts -> new CompositionLotsResponse.LotPropose(
                        parts.stream().map(pt -> partProposee(pt, membres, nbTours)).toList()))
                .toList();
        List<CompositionLotsResponse.PartProposee> restes = p.restes().stream()
                .map(pt -> partProposee(pt, membres, nbTours)).toList();
        return new CompositionLotsResponse(lot, nbTours, lot.multiply(BigDecimal.valueOf(nbTours)),
                p.complete(), lots, restes);
    }

    /**
     * Fige la session : constitue les lots/tours, calcule la cagnotte, tire l'ordre au hasard.
     * Avec {@code composition}, applique celle du bureau après contrôle ; sinon compose
     * automatiquement. Refuse de figer tant qu'un reliquat ne rentre dans aucun lot exact.
     * Idempotent : ne fait rien si déjà figée.
     */
    @Transactional
    public SessionTontine figer(UUID sessionId, FigerLotRequest composition) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        exigerModeLot(session);
        if (session.isFigee()) {
            return session;
        }

        BigDecimal lot = montantLot(session);
        List<ParticipationLot> participations = participationLotRepository.findAllBySessionId(sessionId);
        if (participations.isEmpty()) {
            throw new IllegalStateException("Aucune adhésion à figer pour cette session");
        }
        Map<UUID, Membre> membres = membresParId(participations);

        List<List<CompositionLots.Part>> lots;
        if (composition != null && composition.lots() != null && !composition.lots().isEmpty()) {
            List<List<CompositionLots.Part>> demandee = composition.lots().stream()
                    .map(l -> l.parts().stream()
                            .map(pt -> new CompositionLots.Part(pt.membreId(), pt.montant()))
                            .toList())
                    .toList();
            lots = CompositionLots.valider(demandee, mises(participations), lot);
        } else {
            CompositionLots.Proposition p = CompositionLots.proposer(mises(participations), lot, new Random());
            if (!p.complete()) {
                String detail = p.restes().stream()
                        .map(r -> nomComplet(membres.get(r.membreId())) + " (" + r.montant().toPlainString() + ")")
                        .collect(java.util.stream.Collectors.joining(", "));
                throw new IllegalStateException("Figeage impossible : ces reliquats ne forment aucun lot de "
                        + lot.toPlainString() + " exact — " + detail
                        + ". Ajustez les mises ou composez les lots manuellement.");
            }
            lots = new ArrayList<>(p.lots());
        }

        int nbTours = lots.size();
        BigDecimal cagnotte = lot.multiply(BigDecimal.valueOf(nbTours));

        // Ordre des tours tiré au hasard ; chaque membre touche sa part × nb de tours
        List<List<CompositionLots.Part>> ordonnes = new ArrayList<>(lots);
        Collections.shuffle(ordonnes);
        int ordre = 1;
        for (List<CompositionLots.Part> parts : ordonnes) {
            OrdreBeneficiaire ob = ordreBeneficiaireRepository.save(OrdreBeneficiaire.builder()
                    .session(session)
                    .membre(membres.get(parts.get(0).membreId()))   // référence indicative ; les parts portent le détail
                    .ordre(ordre++)
                    .beneficie(false)
                    .build());
            for (CompositionLots.Part pt : parts) {
                partLotRepository.save(PartLot.builder()
                        .ordreBeneficiaire(ob)
                        .membre(membres.get(pt.membreId()))
                        .montantMensuel(pt.montant())
                        .partCagnotte(partCagnotte(pt.montant(), nbTours))
                        .build());
            }
        }

        session.setCagnotte(cagnotte);
        session.setNombreMembres(nbTours);
        session.setFigee(true);
        session.setDateFigeage(LocalDate.now());
        SessionTontine saved = sessionRepository.save(session);

        log.info("[Lot] Session {} figée : {} tours, cagnotte {}", sessionId, nbTours, cagnotte);
        return saved;
    }

    /** Vue complète d'une session à lot : adhésions + lots/tours avec parts et versements. */
    @Transactional(readOnly = true)
    public com.tontinepro.tontinepro_backend.api.session.dto.SessionLotResponse getLotView(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        List<ParticipationLot> participations = participationLotRepository.findAllBySessionId(sessionId);
        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId);
        List<PartLot> parts = partLotRepository.findAllByOrdreBeneficiaireSessionId(sessionId);

        Map<UUID, LocalDate> datesTour = new HashMap<>();
        ordres.forEach(ob -> datesTour.put(ob.getId(), dateTour(ob)));
        // Retenue prévue sur les parts pas encore versées (affichée avant le versement)
        Map<UUID, BigDecimal> retenuesPrevues = new HashMap<>();
        for (PartLot p : parts) {
            if (!p.isVerse()) {
                short annee = (short) datesTour.get(p.getOrdreBeneficiaire().getId()).getYear();
                retenuesPrevues.put(p.getId(), retenuePrevue(session.getTontine(), p, annee));
            }
        }
        return com.tontinepro.tontinepro_backend.api.session.dto.SessionLotResponse.build(
                sessionId, session.isFigee(), session.getDateFigeage(),
                session.getTontine().getMontantLot(), session.getCagnotte(),
                session.isFigee() ? ordres.size() : null,
                session.getTresorerieLots(), participations, ordres, parts, datesTour, retenuesPrevues);
    }

    // ── Versements (une part = un co-bénéficiaire payé séparément) ────────────

    /**
     * Verse une part : retient le reste dû du fond de caisse du membre, enregistre
     * le montant remis. Le tour est clos quand toutes ses parts sont versées.
     */
    @Transactional
    public OrdreBeneficiaire verserPart(UUID sessionId, UUID partId) {
        PartLot part = partDeLaSession(sessionId, partId);
        if (part.isVerse()) {
            throw new IllegalStateException(nomComplet(part.getMembre()) + " a déjà reçu sa part pour ce tour");
        }
        OrdreBeneficiaire ob = part.getOrdreBeneficiaire();
        short annee = (short) dateTour(ob).getYear();
        BigDecimal retenue = retenuePrevue(ob.getSession().getTontine(), part, annee);

        part.setVerse(true);
        part.setDateVersement(LocalDate.now());
        part.setMontantRetenue(retenue);
        part.setMontantVerse(part.getPartCagnotte().subtract(retenue));
        part.setAnneeRetenue(annee);
        partLotRepository.save(part);
        majTour(ob);
        return ob;
    }

    /** Annule le versement d'une part (correction) : le tour redevient ouvert. */
    @Transactional
    public void annulerVersementPart(UUID sessionId, UUID partId) {
        PartLot part = partDeLaSession(sessionId, partId);
        if (!part.isVerse()) {
            throw new IllegalStateException("Cette part n'a pas encore été versée — rien à annuler");
        }
        reinitialiser(part);
        majTour(part.getOrdreBeneficiaire());
    }

    /** Verse toutes les parts restantes d'un tour (équivalent de « valider le bénéfice »). */
    @Transactional
    public void verserTour(UUID sessionId, UUID ordreBeneficiaireId) {
        OrdreBeneficiaire ob = tourDeLaSession(sessionId, ordreBeneficiaireId);
        List<PartLot> restantes = partLotRepository.findAllByOrdreBeneficiaireId(ob.getId()).stream()
                .filter(p -> !p.isVerse()).toList();
        if (restantes.isEmpty()) {
            throw new IllegalStateException("Toutes les parts de ce tour ont déjà été versées");
        }
        restantes.forEach(p -> verserPart(sessionId, p.getId()));
    }

    /** Annule tous les versements d'un tour. */
    @Transactional
    public void annulerTour(UUID sessionId, UUID ordreBeneficiaireId) {
        OrdreBeneficiaire ob = tourDeLaSession(sessionId, ordreBeneficiaireId);
        partLotRepository.findAllByOrdreBeneficiaireId(ob.getId()).stream()
                .filter(PartLot::isVerse)
                .forEach(this::reinitialiser);
        majTour(ob);
    }

    /**
     * Retenue sur une part : reste dû du fond de caisse de l'année, diminué de ce qui a
     * déjà été retenu sur les autres parts versées au membre (un membre à plusieurs
     * tours n'est pas prélevé deux fois), plafonné au montant de la part.
     */
    private BigDecimal retenuePrevue(Tontine tontine, PartLot part, short annee) {
        UUID membreId = part.getMembre().getId();
        BigDecimal dejaRetenu = partLotRepository
                .findAllByMembreIdAndVerseTrueAndAnneeRetenue(membreId, annee).stream()
                .filter(p -> !p.getId().equals(part.getId()))
                .map(p -> p.getMontantRetenue() != null ? p.getMontantRetenue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reste = retenueFondCaisse.resteDu(tontine, membreId, annee).subtract(dejaRetenu);
        if (reste.signum() <= 0) return BigDecimal.ZERO;
        return reste.min(part.getPartCagnotte());
    }

    /** Le tour est clos (bénéficié) quand toutes ses parts sont versées. */
    private void majTour(OrdreBeneficiaire ob) {
        List<PartLot> parts = partLotRepository.findAllByOrdreBeneficiaireId(ob.getId());
        boolean complet = !parts.isEmpty() && parts.stream().allMatch(PartLot::isVerse);
        ob.setBeneficie(complet);
        ob.setMontantRecu(complet
                ? parts.stream().map(PartLot::getMontantVerse).reduce(BigDecimal.ZERO, BigDecimal::add)
                : null);
        ordreBeneficiaireRepository.save(ob);
    }

    private void reinitialiser(PartLot part) {
        part.setVerse(false);
        part.setDateVersement(null);
        part.setMontantRetenue(null);
        part.setMontantVerse(null);
        part.setAnneeRetenue(null);
        partLotRepository.save(part);
    }

    /** Date du tour : celle enregistrée, sinon calculée selon la périodicité et l'ordre. */
    LocalDate dateTour(OrdreBeneficiaire ob) {
        if (ob.getDateBenefice() != null) return ob.getDateBenefice();
        SessionTontine session = ob.getSession();
        int nb = Math.max(session.getNombreMembres(), ob.getOrdre());
        List<LocalDate> dates = periodiciteService.calculerDatesBenefice(
                session.getTontine(), session.getDateDebut(), nb);
        int index = ob.getOrdre() - 1;
        return index < dates.size() && dates.get(index) != null
                ? dates.get(index) : session.getDateDebut().plusMonths(index);
    }

    private PartLot partDeLaSession(UUID sessionId, UUID partId) {
        PartLot part = partLotRepository.findById(partId)
                .orElseThrow(() -> new IllegalArgumentException("Part introuvable : " + partId));
        if (!part.getOrdreBeneficiaire().getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Cette part n'appartient pas à la session " + sessionId);
        }
        exigerSessionFigee(part.getOrdreBeneficiaire().getSession());
        return part;
    }

    private OrdreBeneficiaire tourDeLaSession(UUID sessionId, UUID ordreBeneficiaireId) {
        OrdreBeneficiaire ob = ordreBeneficiaireRepository.findById(ordreBeneficiaireId)
                .orElseThrow(() -> new IllegalArgumentException("Tour introuvable : " + ordreBeneficiaireId));
        if (!ob.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Ce tour n'appartient pas à la session " + sessionId);
        }
        exigerSessionFigee(ob.getSession());
        return ob;
    }

    private void exigerSessionFigee(SessionTontine session) {
        exigerModeLot(session);
        if (!session.isFigee()) {
            throw new IllegalStateException("Les lots ne sont pas encore constitués : figez la session d'abord");
        }
    }

    private void exigerModeLot(SessionTontine session) {
        if (session.getTontine().getMode() != Tontine.ModeTontine.A_LOT) {
            throw new IllegalStateException("Cette session n'est pas une tontine à lot");
        }
    }
    private BigDecimal montantLot(SessionTontine session) {
        BigDecimal lot = session.getTontine().getMontantLot();
        if (lot == null || lot.signum() <= 0) {
            throw new IllegalStateException("Montant du lot non configuré sur la tontine");
        }
        return lot;
    }

    private static List<CompositionLots.Mise> mises(List<ParticipationLot> participations) {
        return participations.stream()
                .map(p -> new CompositionLots.Mise(p.getMembre().getId(), p.getMontantMensuel()))
                .toList();
    }

    private static Map<UUID, Membre> membresParId(List<ParticipationLot> participations) {
        Map<UUID, Membre> membres = new HashMap<>();
        participations.forEach(p -> membres.put(p.getMembre().getId(), p.getMembre()));
        return membres;
    }

    private static BigDecimal partCagnotte(BigDecimal montant, int nbTours) {
        return montant.multiply(BigDecimal.valueOf(nbTours)).setScale(2, RoundingMode.HALF_UP);
    }

    private static CompositionLotsResponse.PartProposee partProposee(CompositionLots.Part pt,
                                                                    Map<UUID, Membre> membres, int nbTours) {
        Membre m = membres.get(pt.membreId());
        return new CompositionLotsResponse.PartProposee(pt.membreId(), m.getNom(), m.getPrenom(),
                pt.montant(), partCagnotte(pt.montant(), nbTours));
    }

    private static String nomComplet(Membre m) {
        return m == null ? "?" : (m.getPrenom() + " " + m.getNom()).trim();
    }
}
