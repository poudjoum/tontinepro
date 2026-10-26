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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tour de remboursement d'une session « à lot » : dernier tour, à sa propre date, financé
 * par les membres que le bureau coche (chacun verse sa mise). À la clôture, tous les avoirs
 * gelés des membres partis sont rendus en entier ; l'écart avec ce qui a été collecté est
 * porté par la trésorerie (manque avancé, surplus gardé).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TourRestitutionService {

    private final SessionTontineRepository sessionRepository;
    private final TourRestitutionRepository tourRepository;
    private final TourRestitutionCotisantRepository cotisantRepository;
    private final AbandonLotRepository abandonLotRepository;
    private final MembreRepository membreRepository;
    private final AbandonLotService abandonLotService;

    public record Cotisant(UUID membreId, BigDecimal montant) {}

    /**
     * Programme le tour, ou remplace sa date et ses cotisants tant qu'il n'est pas clos.
     * Un cotisant déjà encaissé et gardé au même montant reste encaissé.
     */
    @Transactional
    public TourRestitution programmer(UUID sessionId, LocalDate dateTour, List<Cotisant> cotisants) {
        SessionTontine session = sessionEnCours(sessionId);
        if (dateTour == null) {
            throw new IllegalArgumentException("Indiquez la date du tour de remboursement");
        }
        if (dateTour.isBefore(session.getDateDebut())) {
            throw new IllegalArgumentException("La date du tour précède le début de la session");
        }
        if (cotisants == null || cotisants.isEmpty()) {
            throw new IllegalArgumentException("Cochez au moins un membre qui cotise à ce tour");
        }
        Set<UUID> partis = abandonLotRepository.findAllBySessionIdOrderByDateAbandon(sessionId).stream()
                .map(a -> a.getMembre().getId()).collect(Collectors.toSet());
        Map<UUID, BigDecimal> montants = new LinkedHashMap<>();
        for (Cotisant c : cotisants) {
            if (c.membreId() == null) throw new IllegalArgumentException("Membre manquant dans la liste des cotisants");
            if (c.montant() == null || c.montant().signum() <= 0) {
                throw new IllegalArgumentException("Chaque cotisant doit verser un montant strictement positif");
            }
            if (partis.contains(c.membreId())) {
                throw new IllegalArgumentException("Un membre parti ne peut pas cotiser au tour de remboursement");
            }
            if (montants.put(c.membreId(), c.montant()) != null) {
                throw new IllegalArgumentException("Un membre apparaît deux fois dans la liste des cotisants");
            }
        }

        TourRestitution tour = tourRepository.findBySessionId(sessionId).orElse(null);
        if (tour == null) {
            if (avoirsGeles(sessionId).isEmpty()) {
                throw new IllegalStateException("Aucun avoir gelé à restituer : pas de tour de remboursement à prévoir");
            }
            tour = TourRestitution.builder().session(session).dateTour(dateTour).build();
        } else if (tour.isClos()) {
            throw new IllegalStateException("Le tour de remboursement est clos : rouvrez-le pour le modifier");
        }
        tour.setDateTour(dateTour);
        tour = tourRepository.save(tour);

        Map<UUID, TourRestitutionCotisant> existants = cotisantRepository.findAllByTourId(tour.getId()).stream()
                .collect(Collectors.toMap(c -> c.getMembre().getId(), c -> c));
        List<TourRestitutionCotisant> retires = existants.values().stream()
                .filter(c -> !montants.containsKey(c.getMembre().getId())).toList();
        cotisantRepository.deleteAll(retires);
        for (Map.Entry<UUID, BigDecimal> e : montants.entrySet()) {
            TourRestitutionCotisant c = existants.get(e.getKey());
            if (c == null) {
                Membre membre = membreRepository.findById(e.getKey())
                        .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + e.getKey()));
                if (!membre.getTontine().getId().equals(session.getTontine().getId())) {
                    throw new IllegalArgumentException("Ce membre n'appartient pas à cette tontine");
                }
                c = TourRestitutionCotisant.builder().tour(tour).membre(membre).build();
            } else if (c.getMontant().compareTo(e.getValue()) != 0) {
                c.setPaye(false);   // montant changé : à réencaisser
                c.setDatePaiement(null);
            }
            c.setMontant(e.getValue());
            cotisantRepository.save(c);
        }
        log.info("[Lot] Tour de remboursement session {} programmé le {} : {} cotisant(s)",
                sessionId, dateTour, montants.size());
        return tour;
    }

    /** Encaisse (ou annule l'encaissement de) la cotisation d'un membre au tour. */
    @Transactional
    public void encaisser(UUID sessionId, UUID cotisantId, boolean paye) {
        TourRestitution tour = tourOuvert(sessionId);
        TourRestitutionCotisant c = cotisantRepository.findById(cotisantId)
                .filter(x -> x.getTour().getId().equals(tour.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Cotisant introuvable dans ce tour : " + cotisantId));
        c.setPaye(paye);
        c.setDatePaiement(paye ? LocalDate.now() : null);
        cotisantRepository.save(c);
    }

    /**
     * Clôt le tour : rend en entier tous les avoirs gelés. Exige que les autres tours soient
     * versés et que chaque cotisant ait payé (retirez de la liste celui qui ne paiera pas).
     */
    @Transactional
    public TourRestitution clore(UUID sessionId) {
        TourRestitution tour = tourOuvert(sessionId);
        if (!abandonLotService.toursClos(sessionId)) {
            throw new IllegalStateException("Versez d'abord tous les autres tours : le remboursement vient en dernier");
        }
        List<TourRestitutionCotisant> cotisants = cotisantRepository.findAllByTourId(tour.getId());
        String impayes = cotisants.stream().filter(c -> !c.isPaye())
                .map(c -> (c.getMembre().getPrenom() + " " + c.getMembre().getNom()).trim())
                .collect(Collectors.joining(", "));
        if (!impayes.isEmpty()) {
            throw new IllegalStateException("Cotisation pas encore encaissée : " + impayes);
        }
        List<AbandonLot> avoirs = avoirsGeles(sessionId);
        if (avoirs.isEmpty()) {
            throw new IllegalStateException("Aucun avoir gelé à restituer");
        }
        LocalDate aujourdhui = LocalDate.now();
        BigDecimal restitue = BigDecimal.ZERO;
        for (AbandonLot a : avoirs) {
            a.setStatut(AbandonLot.Statut.AVOIR_RESTITUE);
            a.setDateRestitution(aujourdhui);
            a.setRestitueParTour(true);
            abandonLotRepository.save(a);
            restitue = restitue.add(a.getSolde());
        }
        BigDecimal collecte = cotisants.stream().map(TourRestitutionCotisant::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        tour.setClos(true);
        tour.setDateCloture(aujourdhui);
        tour.setMontantCollecte(collecte);
        tour.setMontantRestitue(restitue);
        log.info("[Lot] Tour de remboursement session {} clos : collecté {}, restitué {} à {} membre(s)",
                sessionId, collecte, restitue, avoirs.size());
        return tourRepository.save(tour);
    }

    /** Rouvre un tour clos par erreur : les avoirs qu'il a rendus redeviennent gelés. */
    @Transactional
    public void rouvrir(UUID sessionId) {
        sessionEnCours(sessionId);
        TourRestitution tour = tourRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Aucun tour de remboursement pour cette session"));
        if (!tour.isClos()) {
            throw new IllegalStateException("Le tour de remboursement n'est pas clos");
        }
        abandonLotRepository.findAllBySessionIdOrderByDateAbandon(sessionId).stream()
                .filter(AbandonLot::isRestitueParTour)
                .forEach(a -> {
                    a.setStatut(AbandonLot.Statut.AVOIR_GELE);
                    a.setDateRestitution(null);
                    a.setRestitueParTour(false);
                    abandonLotRepository.save(a);
                });
        tour.setClos(false);
        tour.setDateCloture(null);
        tour.setMontantCollecte(null);
        tour.setMontantRestitue(null);
        tourRepository.save(tour);
    }

    /** Supprime un tour pas encore clos (ses cotisants avec). */
    @Transactional
    public void supprimer(UUID sessionId) {
        TourRestitution tour = tourOuvert(sessionId);
        cotisantRepository.deleteAll(cotisantRepository.findAllByTourId(tour.getId()));
        tourRepository.delete(tour);
    }

    private List<AbandonLot> avoirsGeles(UUID sessionId) {
        return abandonLotRepository.findAllBySessionIdOrderByDateAbandon(sessionId).stream()
                .filter(a -> a.getStatut() == AbandonLot.Statut.AVOIR_GELE).toList();
    }

    private TourRestitution tourOuvert(UUID sessionId) {
        sessionEnCours(sessionId);
        TourRestitution tour = tourRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Aucun tour de remboursement pour cette session"));
        if (tour.isClos()) {
            throw new IllegalStateException("Le tour de remboursement est clos : rouvrez-le pour le modifier");
        }
        return tour;
    }

    private SessionTontine sessionEnCours(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        if (session.getTontine().getMode() != Tontine.ModeTontine.A_LOT) {
            throw new IllegalStateException("Cette session n'est pas une tontine à lot");
        }
        if (session.getStatut() != SessionTontine.Statut.EN_COURS) {
            throw new IllegalStateException("La session n'est pas en cours");
        }
        return session;
    }
}
