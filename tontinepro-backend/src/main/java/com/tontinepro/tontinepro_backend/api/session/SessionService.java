package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.cotisation.dto.CotisationResponse;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.api.session.dto.*;
import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.pret.Pret;
import com.tontinepro.tontinepro_backend.domain.pret.PretRepository;
import com.tontinepro.tontinepro_backend.domain.session.OrdreBeneficiaire;
import com.tontinepro.tontinepro_backend.domain.session.OrdreBeneficiaireRepository;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontine;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontineRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionTontineRepository sessionRepository;
    private final OrdreBeneficiaireRepository ordreBeneficiaireRepository;
    private final TontineRepository tontineRepository;
    private final MembreRepository membreRepository;
    private final CotisationRepository cotisationRepository;
    private final SanctionRepository sanctionRepository;
    private final PeriodiciteService periodiciteService;
    private final NotificationService notificationService;
    private final RapportEmailService rapportEmailService;
    private final CompteEpargneRepository compteEpargneRepository;
    private final PretRepository pretRepository;
    private final FondsAideRepository fondsAideRepository;

    @Transactional
    public SessionResponse creerSession(CreerSessionRequest request) {
        Tontine tontine = tontineRepository.findById(request.tontineId())
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + request.tontineId()));

        // Récupérer uniquement les membres TONTINE actifs (les membres AIDE_SOCIALE n'ont pas de slot calendrier)
        List<Membre> membresActifs = membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                tontine.getId(), Membre.Statut.ACTIF, Membre.TypeParticipation.TONTINE);

        if (membresActifs.isEmpty()) {
            throw new IllegalStateException("Aucun membre actif dans la tontine");
        }

        int nombreMembres = membresActifs.size();

        // Déterminer le prochain numéro de session
        int prochainNumero = sessionRepository.findTopByTontineIdOrderByNumeroDesc(tontine.getId())
                .map(s -> s.getNumero() + 1)
                .orElse(1);

        // Calculer dateFin
        LocalDate dateFin = periodiciteService.calculerDateFin(tontine, request.dateDebut(), nombreMembres);

        // Calculer dateProchaineTontine
        LocalDate dateProchaine;
        if (tontine.getTypeReglePeriodicite() == Tontine.TypeReglePeriodicite.DATE_MANUELLE) {
            dateProchaine = tontine.getDateProchaineTontine();
        } else {
            dateProchaine = periodiciteService.prochainDate(
                    tontine.getTypeReglePeriodicite(), tontine.getJourReference(), request.dateDebut().minusDays(1));
        }

        SessionTontine session = SessionTontine.builder()
                .tontine(tontine)
                .numero(prochainNumero)
                .dateDebut(request.dateDebut())
                .dateFin(dateFin)
                .dateProchaineTontine(dateProchaine)
                .nombreMembres(nombreMembres)
                .cibleMembres(request.cibleMembres())
                .build();

        session = sessionRepository.save(session);

        // Construire l'ordre des membres
        List<Membre> ordreOrdonne = construireOrdre(membresActifs, request.ordreMembreIds());

        // Calculer les dates de bénéfice
        List<LocalDate> datesBenefice = periodiciteService.calculerDatesBenefice(
                tontine, request.dateDebut(), nombreMembres);

        // Créer les enregistrements OrdreBeneficiaire
        List<OrdreBeneficiaire> ordres = new ArrayList<>();
        for (int i = 0; i < ordreOrdonne.size(); i++) {
            OrdreBeneficiaire ob = OrdreBeneficiaire.builder()
                    .session(session)
                    .membre(ordreOrdonne.get(i))
                    .ordre(i + 1)
                    .dateBenefice(i < datesBenefice.size() ? datesBenefice.get(i) : null)
                    .build();
            ordres.add(ob);
        }
        ordreBeneficiaireRepository.saveAll(ordres);

        List<OrdreBeneficiaireResponse> beneficiairesResp = ordres.stream()
                .map(OrdreBeneficiaireResponse::from)
                .toList();

        return SessionResponse.from(session, beneficiairesResp);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listerSessions(UUID tontineId) {
        // Utiliser getById pour que les dates manquantes soient comblées dans chaque session
        return sessionRepository.findAllByTontineIdOrderByNumeroDesc(tontineId).stream()
                .map(s -> getById(s.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getById(UUID id) {
        SessionTontine session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + id));

        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(id);

        // Calculer les dates attendues pour chaque position afin de combler les dates nulles
        // (membres ajoutés via recalibrer ou mode DATE_MANUELLE avec dates non saisies)
        List<LocalDate> datesCalculees = periodiciteService.calculerDatesBenefice(
                session.getTontine(), session.getDateDebut(), ordres.size());

        List<OrdreBeneficiaireResponse> beneficiaires = new ArrayList<>();
        for (int i = 0; i < ordres.size(); i++) {
            OrdreBeneficiaire ob = ordres.get(i);
            LocalDate dateEffective = ob.getDateBenefice() != null
                    ? ob.getDateBenefice()
                    : (i < datesCalculees.size() ? datesCalculees.get(i) : null);

            // Mode DATE_MANUELLE sans date saisie : la périodicité ne sait pas calculer
            // la date du tour. On répartit alors par mois (dateDebut + position) pour que
            // chaque tour vise un mois DISTINCT, au lieu de s'écraser sur le mois de début
            // (ce qui faisait pointer saisie/validation/rapport sur le 1er mois).
            if (dateEffective == null) {
                dateEffective = session.getDateDebut().plusMonths(i);
            }

            beneficiaires.add(new OrdreBeneficiaireResponse(
                    ob.getId(),
                    ob.getMembre().getId(),
                    ob.getMembre().getNom(),
                    ob.getMembre().getPrenom(),
                    ob.getMembre().getMatricule(),
                    ob.getMembre().getStatut().name(),
                    ob.getOrdre(),
                    dateEffective,
                    ob.getMontantRecu(),
                    ob.isBeneficie()
            ));
        }

        return SessionResponse.from(session, beneficiaires);
    }

    /**
     * Résout la date (donc le mois) effective d'un tour, de façon cohérente avec
     * {@link #getById} :
     *   1. date de bénéfice explicite si elle existe (l'admin garde la main) ;
     *   2. sinon la date calculée par la règle de périodicité pour cette position ;
     *   3. sinon (DATE_MANUELLE sans date saisie) on répartit par mois :
     *      dateDebut + position, afin que chaque tour vise un mois DISTINCT et non
     *      systématiquement le mois de début de session — sinon saisie, validation et
     *      rapport s'écrasent tous sur le 1er mois.
     */
    private LocalDate resoudreDateTour(OrdreBeneficiaire ob) {
        if (ob.getDateBenefice() != null) {
            return ob.getDateBenefice();
        }
        SessionTontine session = ob.getSession();
        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(session.getId());
        List<LocalDate> datesCalculees = periodiciteService.calculerDatesBenefice(
                session.getTontine(), session.getDateDebut(), ordres.size());
        int index = 0;
        for (int i = 0; i < ordres.size(); i++) {
            if (ordres.get(i).getId().equals(ob.getId())) {
                index = i;
                break;
            }
        }
        LocalDate calculee = index < datesCalculees.size() ? datesCalculees.get(index) : null;
        return calculee != null ? calculee : session.getDateDebut().plusMonths(index);
    }

    @Transactional
    public SessionResponse mettreAJourProchainDate(UUID id, MiseAJourDateRequest request) {
        SessionTontine session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + id));
        session.setDateProchaineTontine(request.dateProchaineTontine());
        sessionRepository.save(session);
        return getById(id);
    }

    @Transactional
    public SessionResponse reordonnerBeneficiaires(UUID sessionId, ReordonnerBeneficiairesRequest request) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        List<OrdreBeneficiaire> existants = ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId);

        // Vérifier que les IDs correspondent aux membres existants
        Set<UUID> membresSessionIds = existants.stream()
                .map(ob -> ob.getMembre().getId())
                .collect(Collectors.toSet());

        if (!membresSessionIds.containsAll(request.ordreMembreIds())
                || request.ordreMembreIds().size() != existants.size()) {
            throw new IllegalArgumentException("La liste des membres ne correspond pas aux membres de la session");
        }

        // Récupérer la tontine pour recalculer les dates
        Tontine tontine = session.getTontine();
        List<LocalDate> datesBenefice = periodiciteService.calculerDatesBenefice(
                tontine, session.getDateDebut(), session.getNombreMembres());

        // Construire un index membre -> OrdreBeneficiaire existant
        Map<UUID, OrdreBeneficiaire> parMembre = existants.stream()
                .collect(Collectors.toMap(ob -> ob.getMembre().getId(), ob -> ob));

        List<OrdreBeneficiaire> aMettre = new ArrayList<>();
        for (int i = 0; i < request.ordreMembreIds().size(); i++) {
            OrdreBeneficiaire ob = parMembre.get(request.ordreMembreIds().get(i));
            ob.setOrdre(i + 1);
            ob.setDateBenefice(i < datesBenefice.size() ? datesBenefice.get(i) : null);
            aMettre.add(ob);
        }
        ordreBeneficiaireRepository.saveAll(aMettre);

        return getById(sessionId);
    }

    @Transactional
    public SessionResponse validerBenefice(UUID sessionId, UUID ordreBeneficiaireId, ValiderBeneficeRequest request) {
        OrdreBeneficiaire ob = ordreBeneficiaireRepository.findById(ordreBeneficiaireId)
                .orElseThrow(() -> new IllegalArgumentException("Enregistrement introuvable : " + ordreBeneficiaireId));

        if (!ob.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Cet enregistrement n'appartient pas à la session " + sessionId);
        }
        if (ob.isBeneficie()) {
            throw new IllegalStateException("Ce membre a déjà bénéficié pour cette session");
        }

        Tontine tontine = ob.getSession().getTontine();

        // Mode « à lot » figé : le bénéficiaire (slot) encaisse la cagnotte entière.
        // La répartition entre membres groupés est déjà portée par les PartLot.
        if (tontine.getMode() == Tontine.ModeTontine.A_LOT
                && ob.getSession().isFigee()
                && ob.getSession().getCagnotte() != null) {
            ob.setBeneficie(true);
            ob.setMontantRecu(ob.getSession().getCagnotte());
            ordreBeneficiaireRepository.save(ob);
            return getById(sessionId);
        }

        // Tontine « à pot » (CLASSIQUE) : on ne peut clôturer un tour (valider le bénéfice)
        // qu'à partir de sa date d'échéance. Avant l'échéance, la saisie des cotisations
        // reste possible mais pas la clôture (sinon le rapport serait produit trop tôt).
        if (tontine.getMode() == Tontine.ModeTontine.CLASSIQUE
                && ob.getDateBenefice() != null
                && ob.getDateBenefice().isAfter(LocalDate.now())) {
            throw new IllegalStateException(
                    "La séance ne peut être clôturée qu'à partir du "
                    + ob.getDateBenefice().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + ". La saisie des montants reçus reste possible avant cette date.");
        }

        // Calcul automatique : Σcotisations(PAYEE) + Σrepas(PAYEE) - fondAideAnnuelMembre
        // Le montant reçu est calculé sur les cotisations du MOIS DU TOUR (dateBenefice),
        // pas sur le mois de début de session (sinon tous les tours d'une session
        // multi-mois seraient calculés sur le premier mois).
        LocalDate dateTour = resoudreDateTour(ob);
        short mois  = (short) dateTour.getMonthValue();
        short annee = (short) dateTour.getYear();

        List<Cotisation> cotisations = cotisationRepository
                .findAllByTontineIdAndMoisAndAnnee(tontine.getId(), mois, annee);

        BigDecimal totalCotisations = cotisations.stream()
                .filter(c -> c.getStatut() == Cotisation.Statut.PAYEE
                          && c.getMembre().getTypeParticipation() == Membre.TypeParticipation.TONTINE)
                .map(c -> c.getMontant() != null ? c.getMontant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRepas = cotisations.stream()
                .filter(c -> c.getStatut() == Cotisation.Statut.PAYEE)
                .map(c -> c.getMontantRepas() != null ? c.getMontantRepas() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fondAideAnnuel = tontine.getMontantFondAideAnnuelMembre() != null
                ? tontine.getMontantFondAideAnnuelMembre() : BigDecimal.ZERO;

        BigDecimal montantRecu = totalCotisations.add(totalRepas).subtract(fondAideAnnuel);
        if (montantRecu.compareTo(BigDecimal.ZERO) < 0) montantRecu = BigDecimal.ZERO;

        ob.setBeneficie(true);
        ob.setMontantRecu(montantRecu);
        ordreBeneficiaireRepository.save(ob);

        SessionResponse response = getById(sessionId);

        // Générer et envoyer le rapport de tour par email à tous les membres (async)
        try {
            RapportTourResponse rapport = getRapportTour(sessionId, ordreBeneficiaireId);
            rapportEmailService.envoyerRapportTour(ob.getSession().getTontine().getId(), rapport);
        } catch (Exception e) {
            // L'envoi d'email ne doit pas bloquer la validation
        }

        return response;
    }

    /**
     * Annule la validation d'un tour : remet le membre comme non bénéficié
     * (beneficie=false, montantRecu=null) afin de pouvoir corriger les cotisations
     * du mois puis revalider. Recalcule la prochaine date de tontine.
     */
    @Transactional
    public SessionResponse annulerBenefice(UUID sessionId, UUID ordreBeneficiaireId) {
        OrdreBeneficiaire ob = ordreBeneficiaireRepository.findById(ordreBeneficiaireId)
                .orElseThrow(() -> new IllegalArgumentException("Enregistrement introuvable : " + ordreBeneficiaireId));

        if (!ob.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Cet enregistrement n'appartient pas à la session " + sessionId);
        }
        if (!ob.isBeneficie()) {
            throw new IllegalStateException("Ce membre n'a pas encore été validé — rien à annuler");
        }

        ob.setBeneficie(false);
        ob.setMontantRecu(null);
        ordreBeneficiaireRepository.save(ob);

        // Recalculer dateProchaineTontine = date la plus proche parmi les non-bénéficiés
        SessionTontine session = ob.getSession();
        ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId).stream()
                .filter(o -> !o.isBeneficie() && o.getDateBenefice() != null)
                .min(Comparator.comparing(OrdreBeneficiaire::getDateBenefice))
                .ifPresent(prochain -> {
                    session.setDateProchaineTontine(prochain.getDateBenefice());
                    sessionRepository.save(session);
                });

        return getById(sessionId);
    }

    /**
     * Supprime une session et toutes ses données pour permettre de recommencer
     * (table rase) : ordre des bénéficiaires + cotisations des mois couverts par la
     * session [dateDebut .. dateFin]. Opération destructive réservée à l'admin/secrétaire.
     */
    @Transactional
    public void supprimerSession(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        if (session.getStatut() != SessionTontine.Statut.EN_COURS) {
            throw new IllegalStateException(
                    "Seule une session en cours peut être supprimée. Une session clôturée ou annulée est conservée pour l'historique.");
        }

        Tontine tontine = session.getTontine();

        // 1. Cotisations des mois couverts par la session
        LocalDate debut = session.getDateDebut();
        LocalDate fin   = session.getDateFin() != null ? session.getDateFin() : debut;
        YearMonth ymDebut = YearMonth.from(debut);
        YearMonth ymFin   = YearMonth.from(fin);
        List<Cotisation> cotisationsASupprimer = new ArrayList<>();
        for (YearMonth ym = ymDebut; !ym.isAfter(ymFin); ym = ym.plusMonths(1)) {
            cotisationsASupprimer.addAll(cotisationRepository.findAllByTontineIdAndMoisAndAnnee(
                    tontine.getId(), (short) ym.getMonthValue(), (short) ym.getYear()));
        }
        if (!cotisationsASupprimer.isEmpty()) {
            cotisationRepository.deleteAll(cotisationsASupprimer);
        }

        // 2. Ordre des bénéficiaires
        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId);
        if (!ordres.isEmpty()) {
            ordreBeneficiaireRepository.deleteAll(ordres);
        }

        // 3. La session elle-même
        sessionRepository.delete(session);
    }

    /**
     * Modifie la date de bénéfice d'un membre dans la session.
     * Interdit si le membre a déjà bénéficié.
     */
    @Transactional
    public SessionResponse mettreAJourDateBenefice(UUID sessionId, UUID ordreBeneficiaireId,
                                                    MettreAJourDateBeneficeRequest request) {
        OrdreBeneficiaire ob = ordreBeneficiaireRepository.findById(ordreBeneficiaireId)
                .orElseThrow(() -> new IllegalArgumentException("Entrée introuvable : " + ordreBeneficiaireId));

        if (!ob.getSession().getId().equals(sessionId)) {
            throw new IllegalArgumentException("Cette entrée n'appartient pas à la session " + sessionId);
        }
        if (ob.isBeneficie()) {
            throw new IllegalStateException("Ce membre a déjà bénéficié — impossible de modifier sa date");
        }

        ob.setDateBenefice(request.dateBenefice());
        ordreBeneficiaireRepository.save(ob);

        // Après mise à jour, recalculer dateProchaineTontine = date la plus proche
        // parmi les membres non encore bénéficiés (tri par date, nulls last)
        SessionTontine session = ob.getSession();
        List<OrdreBeneficiaire> tous = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId);
        tous.stream()
                .filter(o -> !o.isBeneficie() && o.getDateBenefice() != null)
                .min(Comparator.comparing(OrdreBeneficiaire::getDateBenefice))
                .ifPresent(prochain -> {
                    session.setDateProchaineTontine(prochain.getDateBenefice());
                    sessionRepository.save(session);
                });

        return getById(sessionId);
    }

    /**
     * Retourne le calendrier de tour pour le membre connecté dans la session active.
     */
    @Transactional(readOnly = true)
    public MonTourResponse monTour(String email) {
        return monTour(email, null);
    }

    @Transactional(readOnly = true)
    public MonTourResponse monTour(String email, java.util.UUID tontineId) {
        Membre membre = (tontineId != null
                ? membreRepository.findByUserEmailAndTontineId(email, tontineId)
                : membreRepository.findByUserEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));

        // Chercher la session en cours de la tontine du membre
        return sessionRepository
                .findTopByTontineIdOrderByNumeroDesc(membre.getTontine().getId())
                .flatMap(session -> ordreBeneficiaireRepository
                        .findAllBySessionIdOrderByOrdre(session.getId()).stream()
                        .filter(ob -> ob.getMembre().getId().equals(membre.getId()))
                        .findFirst()
                        .map(ob -> new MonTourResponse(
                                session.getId(),
                                session.getNumero(),
                                ob.getOrdre(),
                                session.getNombreMembres(),
                                ob.getDateBenefice(),
                                ob.isBeneficie(),
                                session.getTontine().getNom()
                        )))
                .orElseThrow(() -> new IllegalArgumentException("Aucune session active pour votre tontine"));
    }

    /**
     * Retourne l'écheancier complet d'une session (tous les bénéficiaires avec dates).
     */
    @Transactional(readOnly = true)
    public List<OrdreBeneficiaireResponse> echeancier(UUID sessionId) {
        return ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId)
                .stream()
                .filter(ob -> ob.getMembre().getStatut() != Membre.Statut.RETIRE || ob.isBeneficie())
                .sorted(java.util.Comparator.comparing(
                        ob -> ob.getDateBenefice() == null ? java.time.LocalDate.MAX : ob.getDateBenefice()))
                .map(OrdreBeneficiaireResponse::from)
                .toList();
    }

    /**
     * Ajoute à la session les membres actifs qui n'y figurent pas encore,
     * recalcule dateFin et met à jour nombreMembres.
     * Les membres déjà présents conservent leur ordre et leur statut.
     */
    @Transactional
    public SessionResponse recalibrerMembres(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        if (session.getStatut() == SessionTontine.Statut.TERMINEE) {
            throw new IllegalStateException("Impossible de recalibrer une session terminée");
        }

        Tontine tontine = session.getTontine();

        List<OrdreBeneficiaire> existants = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId);
        Set<UUID> dejaDans = existants.stream()
                .map(ob -> ob.getMembre().getId())
                .collect(Collectors.toSet());

        List<Membre> nouveaux = membreRepository
                .findAllByTontineIdAndStatut(tontine.getId(), Membre.Statut.ACTIF)
                .stream()
                .filter(m -> !dejaDans.contains(m.getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (nouveaux.isEmpty()) {
            return getById(sessionId);
        }

        int prochainOrdre = existants.stream()
                .mapToInt(OrdreBeneficiaire::getOrdre).max().orElse(0) + 1;

        int nouveauTotal = dejaDans.size() + nouveaux.size();
        session.setNombreMembres(nouveauTotal);
        session.setDateFin(periodiciteService.calculerDateFin(tontine, session.getDateDebut(), nouveauTotal));
        sessionRepository.save(session);

        List<LocalDate> toutesDatesBenefice = periodiciteService
                .calculerDatesBenefice(tontine, session.getDateDebut(), nouveauTotal);

        List<OrdreBeneficiaire> ajouts = new ArrayList<>();
        for (int i = 0; i < nouveaux.size(); i++) {
            int ordreGlobal = prochainOrdre + i;
            LocalDate dateBenefice = ordreGlobal <= toutesDatesBenefice.size()
                    ? toutesDatesBenefice.get(ordreGlobal - 1) : null;
            ajouts.add(OrdreBeneficiaire.builder()
                    .session(session)
                    .membre(nouveaux.get(i))
                    .ordre(ordreGlobal)
                    .dateBenefice(dateBenefice)
                    .build());
        }
        ordreBeneficiaireRepository.saveAll(ajouts);

        return getById(sessionId);
    }

    /**
     * Calcule le bilan financier d'une session :
     * - pot tontine brut (Σ montantTontine des cotisations payées ce mois)
     * - fonds d'aide collecté (Σ montantFondAide) → trésorier
     * - dette fonds d'aide du bénéficiaire (obligation annuelle − payé depuis jan)
     * - pot net remis au bénéficiaire
     */
    @Transactional(readOnly = true)
    public SessionBilanResponse calculerBilan(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        Tontine tontine = session.getTontine();
        short mois  = (short) session.getDateDebut().getMonthValue();
        short annee = (short) session.getDateDebut().getYear();

        // Cotisations payées ce mois pour la tontine
        List<Cotisation> cotisations = cotisationRepository
                .findAllByTontineIdAndMoisAndAnnee(tontine.getId(), mois, annee)
                .stream().filter(c -> c.getStatut() == Cotisation.Statut.PAYEE).toList();

        BigDecimal potBrut = cotisations.stream()
                .map(Cotisation::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fondsCollecte = cotisations.stream()
                .map(c -> c.getMontantFondAide() != null ? c.getMontantFondAide() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Bénéficiaire du jour : premier non bénéficié dans l'ordre
        OrdreBeneficiaire prochainOb = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId).stream()
                .filter(ob -> !ob.isBeneficie()).findFirst().orElse(null);

        // Calcul de la dette fonds d'aide du bénéficiaire
        BigDecimal obligation = tontine.getMontantFondAideAnnuelMembre() != null
                ? tontine.getMontantFondAideAnnuelMembre() : BigDecimal.ZERO;
        BigDecimal dettesFondsAide = BigDecimal.ZERO;
        BigDecimal fondAidePayeAnnee = BigDecimal.ZERO;

        UUID benefId = null;
        String benefNom = null, benefPrenom = null, benefMatricule = null;

        if (prochainOb != null) {
            benefId       = prochainOb.getMembre().getId();
            benefNom      = prochainOb.getMembre().getNom();
            benefPrenom   = prochainOb.getMembre().getPrenom();
            benefMatricule = prochainOb.getMembre().getMatricule();

            if (obligation.compareTo(BigDecimal.ZERO) > 0) {
                fondAidePayeAnnee = cotisationRepository
                        .findAllByMembreIdAndAnneeAndStatut(benefId, annee, Cotisation.Statut.PAYEE)
                        .stream()
                        .map(c -> c.getMontantFondAide() != null ? c.getMontantFondAide() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                dettesFondsAide = obligation.subtract(fondAidePayeAnnee);
                if (dettesFondsAide.compareTo(BigDecimal.ZERO) < 0) {
                    dettesFondsAide = BigDecimal.ZERO;
                }
            }
        }

        BigDecimal potNet = potBrut.subtract(dettesFondsAide);
        if (potNet.compareTo(BigDecimal.ZERO) < 0) potNet = BigDecimal.ZERO;

        // Lignes détail membres
        List<Membre> membresActifs = membreRepository.findAllByTontineIdAndStatut(
                tontine.getId(), Membre.Statut.ACTIF);

        Map<UUID, Cotisation> cotParMembre = cotisations.stream()
                .collect(Collectors.toMap(c -> c.getMembre().getId(), c -> c, (a, b) -> a));

        List<SessionBilanResponse.LignePaiementMembre> lignes = membresActifs.stream().map(m -> {
            Cotisation cot = cotParMembre.get(m.getId());
            BigDecimal mt = cot != null ? cot.getMontant() : BigDecimal.ZERO;
            BigDecimal mf = cot != null && cot.getMontantFondAide() != null
                    ? cot.getMontantFondAide() : BigDecimal.ZERO;
            return new SessionBilanResponse.LignePaiementMembre(
                    m.getId(), m.getMatricule(), m.getPrenom() + " " + m.getNom(),
                    mt, mf, mt.add(mf), cot != null
            );
        }).toList();

        return new SessionBilanResponse(
                sessionId, session.getNumero(),
                cotisations.size(), potBrut, fondsCollecte,
                benefId, benefNom, benefPrenom, benefMatricule,
                obligation, fondAidePayeAnnee, dettesFondsAide,
                potNet, lignes
        );
    }

    /**
     * Génère en masse les cotisations EN_ATTENTE pour tous les membres de la session.
     * Idempotent : si une cotisation existe déjà pour un membre sur le mois/année de la session,
     * elle est conservée telle quelle et retournée sans modification.
     */
    @Transactional
    public List<CotisationResponse> genererCotisations(UUID sessionId) {
        return genererCotisations(sessionId, null, null);
    }

    /**
     * Génère les cotisations EN_ATTENTE pour un mois/année donné. Si {@code moisParam}
     * ou {@code anneeParam} est null, on retombe sur le mois de début de session
     * (comportement historique). Utilisé par l'assistant de reprise pour générer les
     * cotisations de chaque mois passé.
     */
    @Transactional
    public List<CotisationResponse> genererCotisations(UUID sessionId, Short moisParam, Short anneeParam) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        Tontine tontine = session.getTontine();
        short mois  = moisParam  != null ? moisParam  : (short) session.getDateDebut().getMonthValue();
        short annee = anneeParam != null ? anneeParam : (short) session.getDateDebut().getYear();

        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId);

        BigDecimal fondMensuel = (tontine.getModeContributionAide() == Tontine.ModeContributionAide.MENSUEL
                && tontine.getMontantCotisationAide() != null)
                ? tontine.getMontantCotisationAide() : BigDecimal.ZERO;

        List<Cotisation> result = new ArrayList<>();

        // ── Membres TONTINE (dans le calendrier) ──────────────────────────────────
        for (OrdreBeneficiaire ob : ordres) {
            Membre membre = ob.getMembre();
            if (membre.getStatut() != Membre.Statut.ACTIF) continue;

            if (cotisationRepository.existsByMembreIdAndMoisAndAnnee(membre.getId(), mois, annee)) {
                cotisationRepository.findAllByMembreId(membre.getId()).stream()
                        .filter(c -> c.getMois() == mois && c.getAnnee() == annee)
                        .findFirst()
                        .ifPresent(result::add);
            } else {
                BigDecimal montant = tontine.getMontantCotisationMin() != null
                        ? tontine.getMontantCotisationMin() : BigDecimal.ZERO;
                Cotisation c = cotisationRepository.save(Cotisation.builder()
                        .membre(membre)
                        .tontine(tontine)
                        .mois(mois)
                        .annee(annee)
                        .montant(montant)
                        .montantFondAide(fondMensuel)
                        .build());
                result.add(c);
            }
        }

        // ── Membres AIDE_SOCIALE ───────────────────────────────────────────────────
        List<Membre> membresAide = membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                tontine.getId(), Membre.Statut.ACTIF, Membre.TypeParticipation.AIDE_SOCIALE);

        for (Membre membre : membresAide) {
            if (membre.getModePaiementAide() == Membre.ModePaiementAide.EN_UNE_FOIS) {
                // Une seule cotisation pour toute la session — créée au premier appel, ignorée ensuite
                if (!cotisationRepository.existsByMembreIdAndMoisAndAnnee(membre.getId(), mois, annee)) {
                    int nbTours = ordres.size() == 0 ? session.getNombreMembres() : ordres.size();
                    BigDecimal totalAide = fondMensuel.multiply(BigDecimal.valueOf(nbTours));
                    Cotisation c = cotisationRepository.save(Cotisation.builder()
                            .membre(membre)
                            .tontine(tontine)
                            .mois(mois)
                            .annee(annee)
                            .montant(BigDecimal.ZERO)
                            .montantFondAide(totalAide)
                            .build());
                    result.add(c);
                }
            } else {
                // MENSUEL : cotisation mensuelle fond uniquement
                if (cotisationRepository.existsByMembreIdAndMoisAndAnnee(membre.getId(), mois, annee)) {
                    cotisationRepository.findAllByMembreId(membre.getId()).stream()
                            .filter(c -> c.getMois() == mois && c.getAnnee() == annee)
                            .findFirst()
                            .ifPresent(result::add);
                } else {
                    Cotisation c = cotisationRepository.save(Cotisation.builder()
                            .membre(membre)
                            .tontine(tontine)
                            .mois(mois)
                            .annee(annee)
                            .montant(BigDecimal.ZERO)
                            .montantFondAide(fondMensuel)
                            .build());
                    result.add(c);
                }
            }
        }

        return result.stream().map(CotisationResponse::from).toList();
    }

    /**
     * Retourne le statut des cotisations de chaque membre pour la période de la session.
     */
    @Transactional(readOnly = true)
    public SessionCotisationsStatutResponse cotisationsStatut(UUID sessionId) {
        return cotisationsStatut(sessionId, null, null);
    }

    /**
     * Statut des cotisations par membre pour un mois/année donné. Si {@code moisParam}
     * ou {@code anneeParam} est null, on retombe sur le mois de début de session.
     */
    @Transactional(readOnly = true)
    public SessionCotisationsStatutResponse cotisationsStatut(UUID sessionId, Short moisParam, Short anneeParam) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        short mois  = moisParam  != null ? moisParam  : (short) session.getDateDebut().getMonthValue();
        short annee = anneeParam != null ? anneeParam : (short) session.getDateDebut().getYear();

        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId);

        // Index cotisations par membreId pour ce mois/annee
        Map<UUID, Cotisation> cotParMembre = cotisationRepository
                .findAllByTontineIdAndMoisAndAnnee(session.getTontine().getId(), mois, annee)
                .stream().collect(Collectors.toMap(c -> c.getMembre().getId(), c -> c, (a, b) -> a));

        List<SessionCotisationsStatutResponse.MembreCotisationStatut> membres = new ArrayList<>();
        int nbPayes = 0, nbEnAttente = 0, nbEnRetard = 0, nbAbsents = 0;

        for (OrdreBeneficiaire ob : ordres) {
            Membre m = ob.getMembre();
            Cotisation cot = cotParMembre.get(m.getId());
            String statut;
            UUID cotId = null;
            BigDecimal montantTontine = null, montantFondAide = null, montantRepas = null;
            String referencePaiement = null;
            if (cot == null) {
                statut = "ABSENTE";
                nbAbsents++;
            } else {
                statut = cot.getStatut().name();
                cotId = cot.getId();
                montantTontine    = cot.getMontant();
                montantFondAide   = cot.getMontantFondAide();
                montantRepas      = cot.getMontantRepas();
                referencePaiement = cot.getReferencePaiement();
                switch (cot.getStatut()) {
                    case PAYEE -> nbPayes++;
                    case EN_RETARD -> nbEnRetard++;
                    default -> nbEnAttente++;
                }
            }
            membres.add(new SessionCotisationsStatutResponse.MembreCotisationStatut(
                    m.getId(), m.getNom(), m.getPrenom(), m.getMatricule(), statut, cotId,
                    montantTontine, montantFondAide, montantRepas, referencePaiement));
        }

        int total = ordres.size();
        Tontine tontine = session.getTontine();
        BigDecimal fondAideDefaut = (tontine.getModeContributionAide() == Tontine.ModeContributionAide.MENSUEL
                && tontine.getMontantCotisationAide() != null)
                ? tontine.getMontantCotisationAide() : null;

        return new SessionCotisationsStatutResponse(
                sessionId, session.getNumero(), mois, annee,
                total, nbPayes, nbEnAttente, nbEnRetard, nbAbsents,
                nbPayes == total && total > 0,
                tontine.getMontantCotisationMin(),
                fondAideDefaut,
                tontine.getModeContributionAide().name(),
                membres
        );
    }

    /**
     * Saisie groupée des paiements d'une séance.
     * Pour chaque cotisation fournie : enregistre le montant tontine + fond d'aide
     * et passe le statut à PAYEE. Les cotisations déjà PAYEE sont ignorées.
     * Seul l'Admin (Secrétaire/Président) peut effectuer cette opération.
     */
    @Transactional
    public SaisieSeanceResult saisirPaiementsSeance(UUID sessionId,
                                                     SaisirPaiementsSeanceRequest request) {
        // Vérifier que la session existe et est en cours
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        if (session.getStatut() != SessionTontine.Statut.EN_COURS) {
            throw new IllegalStateException("La session n'est pas en cours");
        }

        boolean autoriserCorrection = Boolean.TRUE.equals(request.autoriserCorrection());

        int enregistres = 0;
        int dejaPayes   = 0;
        BigDecimal totalTontine  = BigDecimal.ZERO;
        BigDecimal totalFondAide = BigDecimal.ZERO;
        BigDecimal totalRepas    = BigDecimal.ZERO;

        for (SaisirPaiementsSeanceRequest.PaiementMembre pm : request.paiements()) {
            Cotisation cot = cotisationRepository.findById(pm.cotisationId())
                    .orElse(null);
            if (cot == null) continue;

            // En mode normal, on ne réécrit pas une cotisation déjà PAYEE.
            // En mode correction (reprise de tontine), on l'écrase.
            if (cot.getStatut() == Cotisation.Statut.PAYEE && !autoriserCorrection) {
                dejaPayes++;
                continue;
            }

            if (pm.montantTontine() != null)  cot.setMontant(pm.montantTontine());
            if (pm.montantFondAide() != null)  cot.setMontantFondAide(pm.montantFondAide());
            if (pm.montantRepas() != null)     cot.setMontantRepas(pm.montantRepas());

            cot.setStatut(Cotisation.Statut.PAYEE);
            cot.setDatePaiement(pm.datePaiement() != null
                    ? pm.datePaiement() : java.time.OffsetDateTime.now());
            cot.setReferencePaiement(pm.referencePaiement());
            if (pm.moyenPaiement() != null)    cot.setMoyenPaiement(pm.moyenPaiement());
            cotisationRepository.save(cot);

            totalTontine  = totalTontine.add(cot.getMontant());
            totalFondAide = totalFondAide.add(safe(cot.getMontantFondAide()));
            totalRepas    = totalRepas.add(safe(cot.getMontantRepas()));
            enregistres++;

            notificationService.notifier(cot.getMembre().getUser(),
                    com.tontinepro.tontinepro_backend.domain.notification.Notification.Type.COTISATION_PAYEE,
                    "Cotisation enregistrée",
                    "Votre cotisation de %02d/%d (%s FCFA) a été saisie par le Secrétaire."
                            .formatted(cot.getMois(), cot.getAnnee(), cot.getMontant()),
                    cot.getId(), "COTISATION");
        }

        return new SaisieSeanceResult(
                request.paiements().size(), enregistres, dejaPayes,
                totalTontine, totalFondAide, totalRepas);
    }

    /**
     * Clôture une session : passe son statut à TERMINEE.
     * Avertit si des membres n'ont pas encore bénéficié (mais laisse l'admin forcer).
     */
    @Transactional
    public SessionResponse cloturerSession(UUID sessionId, boolean forcer) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        if (session.getStatut() == SessionTontine.Statut.TERMINEE) {
            throw new IllegalStateException("Cette session est déjà clôturée");
        }

        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId);
        long nonBeneficies = ordres.stream().filter(ob -> !ob.isBeneficie()).count();

        if (nonBeneficies > 0 && !forcer) {
            throw new IllegalStateException(
                    nonBeneficies + " membre(s) n'ont pas encore bénéficié. "
                    + "Utilisez forcer=true pour clôturer quand même.");
        }

        session.setStatut(SessionTontine.Statut.TERMINEE);
        SessionResponse response = getById(sessionRepository.save(session).getId());

        // Envoyer le rapport de fin de session par email à tous les membres (async)
        try {
            RapportFinSessionResponse rapport = getRapportFinSession(sessionId);
            rapportEmailService.envoyerRapportFinSession(session.getTontine().getId(), rapport);
        } catch (Exception e) {
            // L'envoi d'email ne doit pas bloquer la clôture
        }

        return response;
    }

    /**
     * Historique des bénéfices reçus par le membre connecté (toutes sessions confondues).
     */
    @Transactional(readOnly = true)
    public List<MonBeneficeResponse> mesBenefices(String email) {
        Membre membre = membreRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));

        return ordreBeneficiaireRepository
                .findAllByMembreIdAndBeneficieTrueOrderByCreatedAtDesc(membre.getId()).stream()
                .map(ob -> new MonBeneficeResponse(
                        ob.getId(),
                        ob.getSession().getId(),
                        ob.getSession().getNumero(),
                        ob.getSession().getTontine().getNom(),
                        ob.getOrdre(),
                        ob.getSession().getNombreMembres(),
                        ob.getDateBenefice(),
                        ob.getMontantRecu()))
                .toList();
    }

    /**
     * Génère le rapport d'un tour : tableau des cotisations/fond/repas/sanctions
     * de tous les membres actifs + bilan du bénéficiaire.
     */
    @Transactional(readOnly = true)
    public RapportTourResponse getRapportTour(UUID sessionId, UUID ordreBeneficiaireId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable"));

        OrdreBeneficiaire ob = ordreBeneficiaireRepository.findById(ordreBeneficiaireId)
                .orElseThrow(() -> new IllegalArgumentException("Tour introuvable"));

        Tontine tontine = session.getTontine();

        java.time.LocalDate dateTour = resoudreDateTour(ob);
        short mois  = (short) dateTour.getMonthValue();
        short annee = (short) dateTour.getYear();

        // Cotisations du mois concerné
        List<Cotisation> cotisations = cotisationRepository
                .findAllByTontineIdAndMoisAndAnnee(tontine.getId(), mois, annee);
        Map<UUID, Cotisation> cotParMembre = cotisations.stream()
                .collect(Collectors.toMap(c -> c.getMembre().getId(), c -> c, (a, b) -> a));

        // Sanctions non payées par membre pour cette tontine
        Map<UUID, BigDecimal> sanctionsParMembre = sanctionRepository
                .findAllByTontineId(tontine.getId()).stream()
                .filter(s -> !s.isPayee())
                .collect(Collectors.groupingBy(
                        s -> s.getMembre().getId(),
                        Collectors.reducing(BigDecimal.ZERO, Sanction::getMontant, BigDecimal::add)));

        // Membres actifs TONTINE uniquement dans le tableau principal
        List<Membre> membres = membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                tontine.getId(), Membre.Statut.ACTIF, Membre.TypeParticipation.TONTINE);

        List<RapportTourResponse.LigneRapport> lignes = membres.stream().map(m -> {
            Cotisation cot = cotParMembre.get(m.getId());
            boolean payee  = cot != null && cot.getStatut() == Cotisation.Statut.PAYEE;
            BigDecimal cotis  = payee ? cot.getMontant()                 : BigDecimal.ZERO;
            BigDecimal fond   = payee ? safe(cot.getMontantFondAide())   : BigDecimal.ZERO;
            BigDecimal repas  = payee ? safe(cot.getMontantRepas())      : BigDecimal.ZERO;
            BigDecimal sanct  = sanctionsParMembre.getOrDefault(m.getId(), BigDecimal.ZERO);
            Cotisation.MoyenPaiement moyen = payee ? cot.getMoyenPaiement() : null;
            return new RapportTourResponse.LigneRapport(
                    m.getId(), m.getPrenom() + " " + m.getNom(), m.getMatricule(),
                    cotis, fond, repas, sanct, payee, moyen);
        }).toList();

        BigDecimal totalCotis  = lignes.stream().map(RapportTourResponse.LigneRapport::cotisation).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFond   = lignes.stream().map(RapportTourResponse.LigneRapport::fond).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRepas  = lignes.stream().map(RapportTourResponse.LigneRapport::repas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSanct  = lignes.stream().map(RapportTourResponse.LigneRapport::sanctions).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Bilan bénéficiaire
        BigDecimal potBrut = totalCotis.add(totalRepas);
        // Fond d'aide versé par le bénéficiaire pour ce tour (sa ligne du tableau) : déduit du pot
        BigDecimal fondBeneficiaire = lignes.stream()
                .filter(l -> l.membreId().equals(ob.getMembre().getId()))
                .map(RapportTourResponse.LigneRapport::fond)
                .findFirst().orElse(BigDecimal.ZERO);
        BigDecimal cagnotte = potBrut.subtract(fondBeneficiaire);
        if (cagnotte.compareTo(BigDecimal.ZERO) < 0) cagnotte = BigDecimal.ZERO;

        // ── Contributeurs Aide Sociale ─────────────────────────────────────────────
        short moisSession = (short) session.getDateDebut().getMonthValue();
        short anneeSession = (short) session.getDateDebut().getYear();

        List<Membre> membresAide = membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                tontine.getId(), Membre.Statut.ACTIF, Membre.TypeParticipation.AIDE_SOCIALE);

        List<RapportTourResponse.ContributeurAideSociale> contributeurs = membresAide.stream().map(m -> {
            // EN_UNE_FOIS : cotisation créée au mois de début de session
            // MENSUEL : cotisation du mois courant
            short moisLookup  = m.getModePaiementAide() == Membre.ModePaiementAide.EN_UNE_FOIS ? moisSession : mois;
            short anneeLookup = m.getModePaiementAide() == Membre.ModePaiementAide.EN_UNE_FOIS ? anneeSession : annee;

            Cotisation cot = cotisationRepository.findAllByMembreId(m.getId()).stream()
                    .filter(c -> c.getMois() == moisLookup && c.getAnnee() == anneeLookup)
                    .findFirst().orElse(null);

            BigDecimal fond = cot != null && cot.getMontantFondAide() != null
                    ? cot.getMontantFondAide() : BigDecimal.ZERO;
            boolean paye = cot != null && cot.getStatut() == Cotisation.Statut.PAYEE;

            return new RapportTourResponse.ContributeurAideSociale(
                    m.getId(), m.getPrenom() + " " + m.getNom(), m.getMatricule(),
                    m.getModePaiementAide(), fond, paye);
        }).toList();

        return new RapportTourResponse(
                sessionId, session.getNumero(), tontine.getNom(),
                dateTour, mois, annee,
                ob.getMembre().getId(),
                ob.getMembre().getPrenom() + " " + ob.getMembre().getNom(),
                ob.getMembre().getMatricule(),
                lignes,
                totalCotis, totalFond, totalRepas, totalSanct,
                potBrut, fondBeneficiaire, cagnotte,
                contributeurs);
    }

    /**
     * Rapport de fin de session : bilan financier complet agrégé sur toute la
     * durée de la session (cotisations, repas, fonds collectés), montants
     * redistribués aux bénéficiaires, snapshot épargne / prêts / fonds de
     * solidarité, et fiche individuelle par membre.
     */
    @Transactional(readOnly = true)
    public RapportFinSessionResponse getRapportFinSession(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        Tontine tontine = session.getTontine();
        UUID tontineId = tontine.getId();

        // Cotisations PAYEE sur l'ensemble des mois couverts par la session
        LocalDate debut = session.getDateDebut();
        LocalDate fin   = session.getDateFin() != null ? session.getDateFin() : debut;
        YearMonth ymDebut = YearMonth.from(debut);
        YearMonth ymFin   = YearMonth.from(fin);

        List<Cotisation> cotisations = new ArrayList<>();
        for (YearMonth ym = ymDebut; !ym.isAfter(ymFin); ym = ym.plusMonths(1)) {
            cotisations.addAll(cotisationRepository.findAllByTontineIdAndMoisAndAnneeAndStatut(
                    tontineId, (short) ym.getMonthValue(), (short) ym.getYear(), Cotisation.Statut.PAYEE));
        }

        BigDecimal totalCotisations = cotisations.stream()
                .map(Cotisation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRepas = cotisations.stream()
                .map(c -> safe(c.getMontantRepas())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFondAide = cotisations.stream()
                .map(c -> safe(c.getMontantFondAide())).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Agrégats de cotisation par membre
        Map<UUID, BigDecimal> cotiseParMembre = new HashMap<>();
        Map<UUID, BigDecimal> fondParMembre   = new HashMap<>();
        Map<UUID, BigDecimal> repasParMembre  = new HashMap<>();
        for (Cotisation c : cotisations) {
            UUID mId = c.getMembre().getId();
            cotiseParMembre.merge(mId, safe(c.getMontant()), BigDecimal::add);
            fondParMembre.merge(mId, safe(c.getMontantFondAide()), BigDecimal::add);
            repasParMembre.merge(mId, safe(c.getMontantRepas()), BigDecimal::add);
        }

        // Bénéfices : ordre des bénéficiaires de la session
        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId);
        int nbToursTotal    = ordres.size();
        int nbToursRealises = (int) ordres.stream().filter(OrdreBeneficiaire::isBeneficie).count();
        BigDecimal totalRedistribue = ordres.stream()
                .filter(OrdreBeneficiaire::isBeneficie)
                .map(ob -> safe(ob.getMontantRecu())).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Épargne : snapshot des soldes par membre
        Map<UUID, BigDecimal> epargneParMembre = new HashMap<>();
        BigDecimal totalEpargne = BigDecimal.ZERO;
        for (CompteEpargne ce : compteEpargneRepository.findAllByMembreTontineId(tontineId)) {
            epargneParMembre.put(ce.getMembre().getId(), safe(ce.getSolde()));
            totalEpargne = totalEpargne.add(safe(ce.getSolde()));
        }

        // Prêts : décaissé / intérêts générés / en cours
        Map<UUID, BigDecimal> pretEnCoursParMembre = new HashMap<>();
        BigDecimal pretsTotalDecaisse   = BigDecimal.ZERO;
        BigDecimal pretsInteretsGeneres = BigDecimal.ZERO;
        BigDecimal pretsEnCours         = BigDecimal.ZERO;
        for (Pret p : pretRepository.findAllByMembreTontineId(tontineId)) {
            UUID mId = p.getMembre().getId();
            switch (p.getStatut()) {
                case EN_COURS -> {
                    pretsTotalDecaisse = pretsTotalDecaisse.add(safe(p.getMontantPrincipal()));
                    pretsEnCours       = pretsEnCours.add(safe(p.getMontantTotal()));
                    pretEnCoursParMembre.merge(mId, safe(p.getMontantTotal()), BigDecimal::add);
                }
                case SOLDE -> {
                    pretsTotalDecaisse  = pretsTotalDecaisse.add(safe(p.getMontantPrincipal()));
                    pretsInteretsGeneres = pretsInteretsGeneres.add(
                            safe(p.getMontantTotal()).subtract(safe(p.getMontantPrincipal())));
                }
                default -> { /* DEMANDE / VALIDE / REJETE : non décaissés */ }
            }
        }

        BigDecimal soldeFondsSolidarite = fondsAideRepository.findByTontineId(tontineId)
                .map(f -> safe(f.getSolde())).orElse(BigDecimal.ZERO);

        // Fiches individuelles (membres du calendrier de la session)
        List<RapportFinSessionResponse.FicheMembre> fiches = ordres.stream().map(ob -> {
            Membre m = ob.getMembre();
            UUID mId = m.getId();
            return new RapportFinSessionResponse.FicheMembre(
                    mId, m.getMatricule(), m.getPrenom() + " " + m.getNom(),
                    cotiseParMembre.getOrDefault(mId, BigDecimal.ZERO),
                    fondParMembre.getOrDefault(mId, BigDecimal.ZERO),
                    repasParMembre.getOrDefault(mId, BigDecimal.ZERO),
                    ob.isBeneficie(),
                    ob.getDateBenefice(),
                    safe(ob.getMontantRecu()),
                    epargneParMembre.getOrDefault(mId, BigDecimal.ZERO),
                    pretEnCoursParMembre.getOrDefault(mId, BigDecimal.ZERO));
        }).toList();

        return new RapportFinSessionResponse(
                sessionId, session.getNumero(), tontine.getNom(),
                debut, session.getDateFin(), session.getStatut().name(),
                session.getNombreMembres(), nbToursRealises, nbToursTotal,
                totalCotisations, totalRepas, totalFondAide, totalRedistribue,
                soldeFondsSolidarite, totalEpargne,
                pretsTotalDecaisse, pretsInteretsGeneres, pretsEnCours,
                fiches);
    }

    // ─── Inscription en retard ──────────────────────────────────────────────────

    /**
     * Liste les membres actifs de la tontine non encore inscrits dans la session,
     * avec le détail du rattrapage à effectuer pour chaque tour déjà complété.
     */
    @Transactional(readOnly = true)
    public List<MembreEligibleRetardResponse> membresEligiblesRetard(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable"));
        if (session.getStatut() != SessionTontine.Statut.EN_COURS) {
            throw new IllegalStateException("La session n'est pas en cours");
        }

        List<OrdreBeneficiaire> toursCompletes =
                ordreBeneficiaireRepository.findAllBySessionIdAndBeneficieTrueOrderByOrdreAsc(sessionId);

        Tontine tontine = session.getTontine();
        List<Membre> membresActifs = membreRepository.findAllByTontineIdAndStatut(tontine.getId(), Membre.Statut.ACTIF);

        Set<UUID> dejaDansSession = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId).stream()
                .map(ob -> ob.getMembre().getId())
                .collect(Collectors.toSet());

        return membresActifs.stream()
                .filter(m -> {
                    if (!dejaDansSession.contains(m.getId())) {
                        // Pas encore dans la session : éligible si au moins un tour est clôturé
                        return !toursCompletes.isEmpty();
                    }
                    // Déjà dans la session (recalibrer) : éligible si une cotisation rétroactive manque
                    return toursCompletes.stream().anyMatch(ob -> {
                        LocalDate dt = resoudreDateTour(ob);
                        return !cotisationRepository.existsByMembreIdAndMoisAndAnnee(
                                m.getId(), (short) dt.getMonthValue(), (short) dt.getYear());
                    });
                })
                .map(m -> buildEligibleResponse(m, toursCompletes, tontine))
                .toList();
    }

    private MembreEligibleRetardResponse buildEligibleResponse(Membre membre,
            List<OrdreBeneficiaire> toursCompletes, Tontine tontine) {

        List<MembreEligibleRetardResponse.DetailTourRattrapage> details = new ArrayList<>();
        BigDecimal totalRattrapage = BigDecimal.ZERO;
        BigDecimal cotisUnitaire   = BigDecimal.ZERO;
        BigDecimal repasUnitaire   = BigDecimal.ZERO;

        for (OrdreBeneficiaire ob : toursCompletes) {
            LocalDate dateTour = resoudreDateTour(ob);
            short mois  = (short) dateTour.getMonthValue();
            short annee = (short) dateTour.getYear();

            BigDecimal avgCotis = moyenneCotisations(tontine.getId(), mois, annee, false);
            BigDecimal avgRepas = moyenneRepas(tontine.getId(), mois, annee);
            if (avgCotis.compareTo(BigDecimal.ZERO) == 0 && tontine.getMontantCotisationMin() != null) {
                avgCotis = tontine.getMontantCotisationMin();
            }

            BigDecimal total = avgCotis.add(avgRepas);
            totalRattrapage  = totalRattrapage.add(total);
            cotisUnitaire    = avgCotis;
            repasUnitaire    = avgRepas;

            details.add(new MembreEligibleRetardResponse.DetailTourRattrapage(
                    ob.getOrdre(),
                    ob.getMembre().getPrenom() + " " + ob.getMembre().getNom(),
                    dateTour.toString(),
                    avgCotis, avgRepas, total));
        }

        return new MembreEligibleRetardResponse(
                membre.getId(), membre.getNom(), membre.getPrenom(), membre.getMatricule(),
                toursCompletes.size(), cotisUnitaire, repasUnitaire, totalRattrapage, details);
    }

    /**
     * Inscrit un membre en retard dans la session :
     * - crée des cotisations PAYEE rétroactives pour chaque tour passé
     * - crédite le montantRecu de chaque bénéficiaire passé du complément
     * - ajoute le membre en fin de calendrier
     * - renvoie les rapports PDF actualisés par email à tous les membres
     */
    @Transactional
    public InscrireEnRetardResult inscrireEnRetard(UUID sessionId, UUID membreId,
                                                    InscrireEnRetardRequest overrides) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable"));
        if (session.getStatut() != SessionTontine.Statut.EN_COURS) {
            throw new IllegalStateException("La session n'est pas en cours");
        }

        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable"));
        if (!membre.getTontine().getId().equals(session.getTontine().getId())) {
            throw new IllegalArgumentException("Ce membre n'appartient pas à cette tontine");
        }
        boolean dejaDansSession = ordreBeneficiaireRepository.existsBySessionIdAndMembreId(sessionId, membreId);

        List<OrdreBeneficiaire> toursCompletes =
                ordreBeneficiaireRepository.findAllBySessionIdAndBeneficieTrueOrderByOrdreAsc(sessionId);

        if (toursCompletes.size() >= 3) {
            throw new IllegalStateException(
                    toursCompletes.size() + " tour(s) déjà clôturé(s). "
                    + "L'inscription en retard n'est plus possible après le 2ème tour.");
        }

        Tontine tontine = session.getTontine();
        List<MembreEligibleRetardResponse.DetailTourRattrapage> details = new ArrayList<>();
        BigDecimal totalRattrapage = BigDecimal.ZERO;

        for (OrdreBeneficiaire obPassé : toursCompletes) {
            LocalDate dateTour = resoudreDateTour(obPassé);
            short mois  = (short) dateTour.getMonthValue();
            short annee = (short) dateTour.getYear();

            BigDecimal cotis = (overrides != null && overrides.montantCotisationParTour() != null)
                    ? overrides.montantCotisationParTour()
                    : moyenneCotisations(tontine.getId(), mois, annee, false);
            BigDecimal repas = (overrides != null && overrides.montantRepasParTour() != null)
                    ? overrides.montantRepasParTour()
                    : moyenneRepas(tontine.getId(), mois, annee);
            if (cotis.compareTo(BigDecimal.ZERO) == 0 && overrides == null && tontine.getMontantCotisationMin() != null) {
                cotis = tontine.getMontantCotisationMin();
            }

            // Fond d'aide : 0 par défaut sur le rattrapage, sauf si explicitement saisi
            BigDecimal fond = (overrides != null && overrides.montantFondAideParTour() != null)
                    ? overrides.montantFondAideParTour() : BigDecimal.ZERO;

            // Cotisation rétroactive (idempotente)
            if (!cotisationRepository.existsByMembreIdAndMoisAndAnnee(membreId, mois, annee)) {
                Cotisation cot = Cotisation.builder()
                        .tontine(tontine)
                        .membre(membre)
                        .mois(mois)
                        .annee(annee)
                        .montant(cotis)
                        .montantFondAide(fond)
                        .montantRepas(repas)
                        .statut(Cotisation.Statut.PAYEE)
                        .datePaiement(java.time.OffsetDateTime.now())
                        .referencePaiement("RATTRAPAGE")
                        .build();
                cotisationRepository.save(cot);
            }

            // Créditer le bénéficiaire passé
            BigDecimal complement = cotis.add(repas);
            BigDecimal ancienMontant = obPassé.getMontantRecu() != null ? obPassé.getMontantRecu() : BigDecimal.ZERO;
            obPassé.setMontantRecu(ancienMontant.add(complement));
            ordreBeneficiaireRepository.save(obPassé);

            totalRattrapage = totalRattrapage.add(complement);
            details.add(new MembreEligibleRetardResponse.DetailTourRattrapage(
                    obPassé.getOrdre(),
                    obPassé.getMembre().getPrenom() + " " + obPassé.getMembre().getNom(),
                    dateTour.toString(), cotis, repas, complement));
        }

        // Ajouter en fin de calendrier (seulement si pas déjà dans la session)
        List<OrdreBeneficiaire> tousOrdres = ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId);
        int prochainOrdre;
        LocalDate dateNouveauMembre = null;

        if (!dejaDansSession) {
            prochainOrdre = tousOrdres.size() + 1;

            LocalDate derniereDateExistante = tousOrdres.stream()
                    .map(OrdreBeneficiaire::getDateBenefice)
                    .filter(java.util.Objects::nonNull)
                    .max(java.util.Comparator.naturalOrder())
                    .orElse(session.getDateDebut());

            try {
                dateNouveauMembre = periodiciteService.prochainDate(
                        tontine.getTypeReglePeriodicite(), tontine.getJourReference(), derniereDateExistante);
            } catch (Exception ignored) {}

            ordreBeneficiaireRepository.save(OrdreBeneficiaire.builder()
                    .session(session)
                    .membre(membre)
                    .ordre(prochainOrdre)
                    .dateBenefice(dateNouveauMembre)
                    .beneficie(false)
                    .montantRecu(null)
                    .build());

            session.setNombreMembres(session.getNombreMembres() + 1);
            if (dateNouveauMembre != null && (session.getDateFin() == null || dateNouveauMembre.isAfter(session.getDateFin()))) {
                session.setDateFin(dateNouveauMembre);
            }
            sessionRepository.save(session);
        } else {
            // Membre déjà dans le calendrier (ajouté par recalibrer) — on récupère sa position
            prochainOrdre = tousOrdres.stream()
                    .filter(ob -> ob.getMembre().getId().equals(membreId))
                    .mapToInt(OrdreBeneficiaire::getOrdre)
                    .findFirst().orElse(tousOrdres.size());
            dateNouveauMembre = tousOrdres.stream()
                    .filter(ob -> ob.getMembre().getId().equals(membreId))
                    .map(OrdreBeneficiaire::getDateBenefice)
                    .findFirst().orElse(null);
        }

        // Renvoyer les rapports des tours passés par email (async)
        final UUID tontineId = tontine.getId();
        for (OrdreBeneficiaire obPassé : toursCompletes) {
            try {
                RapportTourResponse rapport = getRapportTour(sessionId, obPassé.getId());
                rapportEmailService.envoyerRapportTour(tontineId, rapport);
            } catch (Exception ignored) {}
        }

        SessionResponse sessionMAJ = getById(sessionId);
        return new InscrireEnRetardResult(
                membre.getId(), membre.getPrenom() + " " + membre.getNom(), membre.getMatricule(),
                prochainOrdre, dateNouveauMembre, toursCompletes.size(),
                totalRattrapage, details, sessionMAJ);
    }

    private BigDecimal moyenneCotisations(UUID tontineId, short mois, short annee, boolean fondSeulement) {
        List<Cotisation> cotis = cotisationRepository
                .findAllByTontineIdAndMoisAndAnneeAndStatut(tontineId, mois, annee, Cotisation.Statut.PAYEE);
        if (cotis.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = cotis.stream().map(Cotisation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(cotis.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal moyenneRepas(UUID tontineId, short mois, short annee) {
        List<Cotisation> cotis = cotisationRepository
                .findAllByTontineIdAndMoisAndAnneeAndStatut(tontineId, mois, annee, Cotisation.Statut.PAYEE);
        if (cotis.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = cotis.stream().map(c -> safe(c.getMontantRepas())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(cotis.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal moyenneFond(UUID tontineId, short mois, short annee) {
        List<Cotisation> cotis = cotisationRepository
                .findAllByTontineIdAndMoisAndAnneeAndStatut(tontineId, mois, annee, Cotisation.Statut.PAYEE);
        if (cotis.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = cotis.stream().map(c -> safe(c.getMontantFondAide())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(cotis.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private List<Membre> construireOrdre(List<Membre> membres, List<UUID> ordreMembreIds) {
        if (ordreMembreIds == null || ordreMembreIds.isEmpty()) {
            // Ordre aléatoire
            List<Membre> melange = new ArrayList<>(membres);
            Collections.shuffle(melange);
            return melange;
        }

        // Valider que tous les IDs fournis sont des membres actifs de la tontine
        Map<UUID, Membre> parId = membres.stream()
                .collect(Collectors.toMap(Membre::getId, m -> m));

        List<Membre> ordonne = new ArrayList<>();
        for (UUID id : ordreMembreIds) {
            Membre m = parId.get(id);
            if (m == null) {
                throw new IllegalArgumentException("Membre introuvable ou inactif dans la tontine : " + id);
            }
            ordonne.add(m);
        }

        // Ajouter les membres non spécifiés à la fin (ordre aléatoire)
        Set<UUID> specifies = new HashSet<>(ordreMembreIds);
        List<Membre> nonSpecifies = membres.stream()
                .filter(m -> !specifies.contains(m.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(nonSpecifies);
        ordonne.addAll(nonSpecifies);

        return ordonne;
    }
}
