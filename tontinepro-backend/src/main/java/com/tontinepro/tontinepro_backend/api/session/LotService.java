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

    /** Vue complète d'une session à lot : adhésions + lots/tours avec parts. */
    @Transactional(readOnly = true)
    public com.tontinepro.tontinepro_backend.api.session.dto.SessionLotResponse getLotView(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        List<ParticipationLot> participations = participationLotRepository.findAllBySessionId(sessionId);
        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId);
        List<PartLot> parts = partLotRepository.findAllByOrdreBeneficiaireSessionId(sessionId);
        return com.tontinepro.tontinepro_backend.api.session.dto.SessionLotResponse.build(
                sessionId, session.isFigee(), session.getDateFigeage(),
                session.getTontine().getMontantLot(), session.getCagnotte(),
                session.isFigee() ? ordres.size() : null,
                session.getTresorerieLots(), participations, ordres, parts);
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
