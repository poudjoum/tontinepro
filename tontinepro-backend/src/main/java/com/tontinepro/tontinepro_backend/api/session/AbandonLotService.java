package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.session.*;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Abandon d'un membre en cours de session « à lot ».
 *
 * <p>Solde = ce qu'il a réellement cotisé dans la session − les parts déjà reçues :
 * <ul>
 *   <li>positif : avoir gelé, restitué en fin de session (une fois tous les tours clos) —
 *       c'est ce qui équilibre les comptes sans coûter au groupe ;</li>
 *   <li>négatif : dette envers le groupe, remboursée au fil de l'eau.</li>
 * </ul>
 * Ses parts non versées sont annulées ; les autres membres de son lot gardent les leurs.
 * Ses cotisations non payées à partir du mois de l'abandon sont supprimées.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbandonLotService {

    private final SessionTontineRepository sessionRepository;
    private final ParticipationLotRepository participationLotRepository;
    private final PartLotRepository partLotRepository;
    private final AbandonLotRepository abandonLotRepository;
    private final CotisationRepository cotisationRepository;
    private final LotService lotService;

    @Transactional
    public AbandonLot declarer(UUID sessionId, UUID membreId, LocalDate date, String motif) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        if (session.getTontine().getMode() != Tontine.ModeTontine.A_LOT) {
            throw new IllegalStateException("Cette session n'est pas une tontine à lot");
        }
        if (session.getStatut() != SessionTontine.Statut.EN_COURS) {
            throw new IllegalStateException("La session n'est pas en cours");
        }
        ParticipationLot participation = participationLotRepository.findBySessionIdAndMembreId(sessionId, membreId)
                .orElseThrow(() -> new IllegalArgumentException("Ce membre n'a pas de mise dans cette session"));
        if (participation.isAbandonnee() || abandonLotRepository.existsBySessionIdAndMembreId(sessionId, membreId)) {
            throw new IllegalStateException("L'abandon de ce membre est déjà enregistré");
        }
        LocalDate dateAbandon = date != null ? date : LocalDate.now();
        if (dateAbandon.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date d'abandon ne peut pas être dans le futur");
        }
        if (dateAbandon.isBefore(session.getDateDebut())) {
            throw new IllegalArgumentException("La date d'abandon précède le début de la session");
        }

        // 1. Ce qu'il a réellement payé dans la session jusqu'à l'abandon
        List<Cotisation> cotisations = cotisationRepository.findAllByMembreId(membreId).stream()
                .filter(c -> c.getTontine().getId().equals(session.getTontine().getId()))
                .toList();
        List<Cotisation> payees = cotisations.stream()
                .filter(c -> c.getStatut() == Cotisation.Statut.PAYEE
                        && LotService.dansSession(session, c, dateAbandon))
                .toList();
        BigDecimal totalCotise = payees.stream()
                .map(c -> c.getMontant() != null ? c.getMontant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<YearMonth> moisPayes = new HashSet<>();
        payees.forEach(c -> moisPayes.add(YearMonth.of(c.getAnnee(), c.getMois())));

        // 2. Parts déjà reçues ; les autres sont annulées et leurs tours recalculés
        BigDecimal totalRecu = BigDecimal.ZERO;
        Set<OrdreBeneficiaire> tours = new HashSet<>();
        for (PartLot part : partsDuMembre(sessionId, membreId)) {
            if (part.isVerse()) {
                totalRecu = totalRecu.add(part.getPartCagnotte() != null ? part.getPartCagnotte() : BigDecimal.ZERO);
            } else if (!part.isAnnulee()) {
                part.setAnnulee(true);
                partLotRepository.save(part);
                tours.add(part.getOrdreBeneficiaire());
            }
        }
        tours.forEach(lotService::majTour);

        // 3. Plus de cotisation attendue à partir du mois de l'abandon
        YearMonth moisAbandon = YearMonth.from(dateAbandon);
        List<Cotisation> aSupprimer = cotisations.stream()
                .filter(c -> c.getStatut() != Cotisation.Statut.PAYEE
                        && !YearMonth.of(c.getAnnee(), c.getMois()).isBefore(moisAbandon)
                        && LotService.dansSession(session, c, null))
                .toList();
        cotisationRepository.deleteAll(aSupprimer);

        participation.setAbandonnee(true);
        participationLotRepository.save(participation);

        BigDecimal solde = totalCotise.subtract(totalRecu);
        AbandonLot abandon = abandonLotRepository.save(AbandonLot.builder()
                .session(session)
                .membre(participation.getMembre())
                .dateAbandon(dateAbandon)
                .motif(motif != null && !motif.isBlank() ? motif.trim() : null)
                .moisCotises(moisPayes.size())
                .totalCotise(totalCotise)
                .totalRecu(totalRecu)
                .solde(solde)
                .statut(statutInitial(solde))
                .build());

        log.info("[Lot] Abandon membre {} session {} : cotisé {}, reçu {}, solde {}, {} part(s) annulée(s)",
                membreId, sessionId, totalCotise, totalRecu, solde, tours.size());
        return abandon;
    }

    /** Restitue l'avoir gelé : en fin de session, une fois tous les tours clos. */
    @Transactional
    public AbandonLot restituer(UUID sessionId, UUID abandonId) {
        AbandonLot a = abandonDeLaSession(sessionId, abandonId);
        if (a.getStatut() != AbandonLot.Statut.AVOIR_GELE) {
            throw new IllegalStateException("Aucun avoir gelé à restituer pour ce membre");
        }
        boolean toursClos = partLotRepository.findAllByOrdreBeneficiaireSessionId(sessionId).stream()
                .allMatch(p -> p.isVerse() || p.isAnnulee());
        if (!toursClos) {
            throw new IllegalStateException(
                    "L'avoir gelé se restitue en fin de session : tous les tours doivent d'abord être versés");
        }
        a.setStatut(AbandonLot.Statut.AVOIR_RESTITUE);
        a.setDateRestitution(LocalDate.now());
        return abandonLotRepository.save(a);
    }

    /** Enregistre un remboursement sur la dette d'un membre parti après son tour. */
    @Transactional
    public AbandonLot rembourser(UUID sessionId, UUID abandonId, BigDecimal montant) {
        AbandonLot a = abandonDeLaSession(sessionId, abandonId);
        if (a.getStatut() != AbandonLot.Statut.DETTE) {
            throw new IllegalStateException("Ce membre n'a pas de dette en cours");
        }
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant remboursé doit être strictement positif");
        }
        BigDecimal reste = a.resteARembourser();
        if (montant.compareTo(reste) > 0) {
            throw new IllegalArgumentException("Le remboursement dépasse la dette restante : "
                    + reste.toPlainString() + " restant, " + montant.toPlainString() + " saisi");
        }
        a.setMontantRembourse(a.getMontantRembourse().add(montant));
        if (a.resteARembourser().signum() == 0) {
            a.setStatut(AbandonLot.Statut.DETTE_SOLDEE);
        }
        return abandonLotRepository.save(a);
    }

    /**
     * Annule un abandon saisi par erreur : le membre redevient actif et ses parts
     * annulées sont rétablies. Ses cotisations à venir sont à régénérer.
     */
    @Transactional
    public void annuler(UUID sessionId, UUID abandonId) {
        AbandonLot a = abandonDeLaSession(sessionId, abandonId);
        if (a.getStatut() == AbandonLot.Statut.AVOIR_RESTITUE
                || a.getStatut() == AbandonLot.Statut.DETTE_SOLDEE
                || a.getMontantRembourse().signum() > 0) {
            throw new IllegalStateException(
                    "Impossible d'annuler : de l'argent a déjà été restitué ou remboursé pour cet abandon");
        }
        UUID membreId = a.getMembre().getId();
        Set<OrdreBeneficiaire> tours = new HashSet<>();
        for (PartLot part : partsDuMembre(sessionId, membreId)) {
            if (part.isAnnulee()) {
                part.setAnnulee(false);
                partLotRepository.save(part);
                tours.add(part.getOrdreBeneficiaire());
            }
        }
        tours.forEach(lotService::majTour);
        participationLotRepository.findBySessionIdAndMembreId(sessionId, membreId).ifPresent(p -> {
            p.setAbandonnee(false);
            participationLotRepository.save(p);
        });
        abandonLotRepository.delete(a);
    }

    static AbandonLot.Statut statutInitial(BigDecimal solde) {
        int signe = solde.signum();
        return signe > 0 ? AbandonLot.Statut.AVOIR_GELE
                : signe < 0 ? AbandonLot.Statut.DETTE
                : AbandonLot.Statut.SOLDE_NUL;
    }

    private List<PartLot> partsDuMembre(UUID sessionId, UUID membreId) {
        return partLotRepository.findAllByOrdreBeneficiaireSessionId(sessionId).stream()
                .filter(p -> p.getMembre().getId().equals(membreId))
                .toList();
    }

    private AbandonLot abandonDeLaSession(UUID sessionId, UUID abandonId) {
        AbandonLot a = abandonLotRepository.findById(abandonId)
                .orElseThrow(() -> new IllegalArgumentException("Abandon introuvable : " + abandonId));
        if (!a.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Cet abandon n'appartient pas à la session " + sessionId);
        }
        return a;
    }
}
