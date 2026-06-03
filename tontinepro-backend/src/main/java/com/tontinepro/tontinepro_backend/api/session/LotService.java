package com.tontinepro.tontinepro_backend.api.session;

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
 * (constitution des lots/tours, cagnotte, regroupement aléatoire des partiels).
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
     * Fige la session : constitue les lots, calcule la cagnotte et le nombre de tours,
     * regroupe aléatoirement les mises partielles, tire l'ordre au hasard.
     * Idempotent : ne fait rien si déjà figée.
     */
    @Transactional
    public SessionTontine figer(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        exigerModeLot(session);
        if (session.isFigee()) {
            return session;
        }

        BigDecimal lot = session.getTontine().getMontantLot();
        if (lot == null || lot.signum() <= 0) {
            throw new IllegalStateException("Montant du lot non configuré sur la tontine");
        }

        List<ParticipationLot> participations = participationLotRepository.findAllBySessionId(sessionId);
        if (participations.isEmpty()) {
            throw new IllegalStateException("Aucune adhésion à figer pour cette session");
        }

        // 1. Décomposer chaque mise en lots pleins (= lot) + reliquat
        // Un "slot" est une liste de contributions (membre, montant alloué à ce slot).
        List<List<Contribution>> slots = new ArrayList<>();
        List<Contribution> reliquats = new ArrayList<>();

        for (ParticipationLot p : participations) {
            BigDecimal mise = p.getMontantMensuel();
            BigDecimal pleins = mise.divideToIntegralValue(lot);   // nb de lots pleins
            int nbPleins = pleins.intValue();
            for (int i = 0; i < nbPleins; i++) {
                slots.add(new ArrayList<>(List.of(new Contribution(p.getMembre(), lot))));
            }
            BigDecimal reliquat = mise.subtract(lot.multiply(pleins));
            if (reliquat.signum() > 0) {
                reliquats.add(new Contribution(p.getMembre(), reliquat));
            }
        }

        // 2. Regrouper aléatoirement les reliquats jusqu'à atteindre un lot
        Collections.shuffle(reliquats);
        List<Contribution> courant = new ArrayList<>();
        BigDecimal cumul = BigDecimal.ZERO;
        for (Contribution c : reliquats) {
            courant.add(c);
            cumul = cumul.add(c.montant());
            if (cumul.compareTo(lot) >= 0) {
                slots.add(new ArrayList<>(courant));
                courant.clear();
                cumul = BigDecimal.ZERO;
            }
        }
        // Reliquats restants non casables → trésorerie (pas de tour créé)
        BigDecimal versTresorerie = BigDecimal.ZERO;
        for (Contribution c : courant) {
            versTresorerie = versTresorerie.add(c.montant());
        }

        int nbTours = slots.size();
        if (nbTours == 0) {
            throw new IllegalStateException(
                    "Aucun lot complet n'a pu être formé avec les mises actuelles (montant du lot trop élevé ?)");
        }
        BigDecimal cagnotte = lot.multiply(BigDecimal.valueOf(nbTours));

        // 3. Tirer l'ordre au hasard et créer les tours + parts
        Collections.shuffle(slots);
        int ordre = 1;
        for (List<Contribution> slot : slots) {
            Membre membreUnique = slot.size() == 1 ? slot.get(0).membre() : slot.get(0).membre();
            OrdreBeneficiaire ob = ordreBeneficiaireRepository.save(OrdreBeneficiaire.builder()
                    .session(session)
                    .membre(membreUnique)   // référence indicative ; les parts portent le détail
                    .ordre(ordre++)
                    .beneficie(false)
                    .build());

            for (Contribution c : slot) {
                // part de cagnotte = mise allouée × nb tours (chacun récupère sa mise × durée)
                BigDecimal part = c.montant().multiply(BigDecimal.valueOf(nbTours))
                        .setScale(2, RoundingMode.HALF_UP);
                partLotRepository.save(PartLot.builder()
                        .ordreBeneficiaire(ob)
                        .membre(c.membre())
                        .montantMensuel(c.montant())
                        .partCagnotte(part)
                        .build());
            }
        }

        session.setCagnotte(cagnotte);
        session.setNombreMembres(nbTours);
        session.setTresorerieLots(session.getTresorerieLots().add(versTresorerie));
        session.setFigee(true);
        session.setDateFigeage(LocalDate.now());
        SessionTontine saved = sessionRepository.save(session);

        log.info("[Lot] Session {} figée : {} tours, cagnotte {}, trésorerie +{}",
                sessionId, nbTours, cagnotte, versTresorerie);
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

    /** Contribution d'un membre à un slot (montant alloué à ce lot). */
    private record Contribution(Membre membre, BigDecimal montant) {}
}
