package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.cotisation.dto.CotisationResponse;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.api.session.dto.*;
import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
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

    @Transactional
    public SessionResponse creerSession(CreerSessionRequest request) {
        Tontine tontine = tontineRepository.findById(request.tontineId())
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + request.tontineId()));

        // Récupérer les membres actifs de la tontine
        List<Membre> membresActifs = membreRepository.findAllByTontineIdAndStatut(
                tontine.getId(), Membre.Statut.ACTIF);

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

            // Si la date calculée est null (mode DATE_MANUELLE), utiliser dateDebut + position
            if (dateEffective == null && i < datesCalculees.size() && datesCalculees.get(i) == null) {
                // Extrapoler depuis le dernier membre qui a une date, ou dateDebut
                LocalDate base = session.getDateDebut();
                dateEffective = base; // fallback minimal
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

        ob.setBeneficie(true);
        ob.setMontantRecu(request.montantRecu());
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
        Membre membre = membreRepository.findByUserEmail(email)
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
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        Tontine tontine = session.getTontine();
        short mois  = (short) session.getDateDebut().getMonthValue();
        short annee = (short) session.getDateDebut().getYear();

        List<OrdreBeneficiaire> ordres = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId);

        List<Cotisation> result = new ArrayList<>();
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
                // Pré-remplir montantFondAide si mode MENSUEL
                BigDecimal fondAide = (tontine.getModeContributionAide() == Tontine.ModeContributionAide.MENSUEL
                        && tontine.getMontantCotisationAide() != null)
                        ? tontine.getMontantCotisationAide() : BigDecimal.ZERO;
                Cotisation c = cotisationRepository.save(Cotisation.builder()
                        .membre(membre)
                        .tontine(tontine)
                        .mois(mois)
                        .annee(annee)
                        .montant(montant)
                        .montantFondAide(fondAide)
                        .build());
                result.add(c);
            }
        }

        return result.stream().map(CotisationResponse::from).toList();
    }

    /**
     * Retourne le statut des cotisations de chaque membre pour la période de la session.
     */
    @Transactional(readOnly = true)
    public SessionCotisationsStatutResponse cotisationsStatut(UUID sessionId) {
        SessionTontine session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));

        short mois  = (short) session.getDateDebut().getMonthValue();
        short annee = (short) session.getDateDebut().getYear();

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
            if (cot == null) {
                statut = "ABSENTE";
                nbAbsents++;
            } else {
                statut = cot.getStatut().name();
                cotId = cot.getId();
                switch (cot.getStatut()) {
                    case PAYEE -> nbPayes++;
                    case EN_RETARD -> nbEnRetard++;
                    default -> nbEnAttente++;
                }
            }
            membres.add(new SessionCotisationsStatutResponse.MembreCotisationStatut(
                    m.getId(), m.getNom(), m.getPrenom(), m.getMatricule(), statut, cotId));
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

        int enregistres = 0;
        int dejaPayes   = 0;
        BigDecimal totalTontine  = BigDecimal.ZERO;
        BigDecimal totalFondAide = BigDecimal.ZERO;
        BigDecimal totalRepas    = BigDecimal.ZERO;

        for (SaisirPaiementsSeanceRequest.PaiementMembre pm : request.paiements()) {
            Cotisation cot = cotisationRepository.findById(pm.cotisationId())
                    .orElse(null);
            if (cot == null) continue;

            if (cot.getStatut() == Cotisation.Statut.PAYEE) {
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
        return getById(sessionRepository.save(session).getId());
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

        java.time.LocalDate dateTour = ob.getDateBenefice() != null
                ? ob.getDateBenefice() : session.getDateDebut();
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

        // Membres actifs (+ ceux qui ont cotisé même si SUSPENDU)
        List<Membre> membres = membreRepository.findAllByTontineIdAndStatut(tontine.getId(), Membre.Statut.ACTIF);

        List<RapportTourResponse.LigneRapport> lignes = membres.stream().map(m -> {
            Cotisation cot = cotParMembre.get(m.getId());
            BigDecimal cotis  = cot != null && cot.getStatut() == Cotisation.Statut.PAYEE ? cot.getMontant()        : BigDecimal.ZERO;
            BigDecimal fond   = cot != null && cot.getStatut() == Cotisation.Statut.PAYEE ? safe(cot.getMontantFondAide()) : BigDecimal.ZERO;
            BigDecimal repas  = cot != null && cot.getStatut() == Cotisation.Statut.PAYEE ? safe(cot.getMontantRepas())    : BigDecimal.ZERO;
            BigDecimal sanct  = sanctionsParMembre.getOrDefault(m.getId(), BigDecimal.ZERO);
            return new RapportTourResponse.LigneRapport(
                    m.getId(), m.getPrenom() + " " + m.getNom(), m.getMatricule(),
                    cotis, fond, repas, sanct,
                    cot != null && cot.getStatut() == Cotisation.Statut.PAYEE);
        }).toList();

        BigDecimal totalCotis  = lignes.stream().map(RapportTourResponse.LigneRapport::cotisation).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFond   = lignes.stream().map(RapportTourResponse.LigneRapport::fond).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRepas  = lignes.stream().map(RapportTourResponse.LigneRapport::repas).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSanct  = lignes.stream().map(RapportTourResponse.LigneRapport::sanctions).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Bilan bénéficiaire
        BigDecimal potBrut = totalCotis.add(totalRepas);
        BigDecimal obligation = tontine.getMontantFondAideAnnuelMembre() != null
                ? tontine.getMontantFondAideAnnuelMembre() : BigDecimal.ZERO;
        BigDecimal fondAidePaye = cotisationRepository
                .findAllByMembreIdAndAnneeAndStatut(ob.getMembre().getId(), annee, Cotisation.Statut.PAYEE)
                .stream().map(c -> safe(c.getMontantFondAide())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dette = obligation.subtract(fondAidePaye);
        if (dette.compareTo(BigDecimal.ZERO) < 0) dette = BigDecimal.ZERO;
        BigDecimal cagnotte = potBrut.subtract(dette);
        if (cagnotte.compareTo(BigDecimal.ZERO) < 0) cagnotte = BigDecimal.ZERO;

        return new RapportTourResponse(
                sessionId, session.getNumero(), tontine.getNom(),
                dateTour, mois, annee,
                ob.getMembre().getId(),
                ob.getMembre().getPrenom() + " " + ob.getMembre().getNom(),
                ob.getMembre().getMatricule(),
                lignes,
                totalCotis, totalFond, totalRepas, totalSanct,
                potBrut, obligation, fondAidePaye, dette, cagnotte);
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
                .filter(m -> !dejaDansSession.contains(m.getId()))
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
            LocalDate dateTour = ob.getDateBenefice() != null ? ob.getDateBenefice() : LocalDate.now();
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
    public InscrireEnRetardResult inscrireEnRetard(UUID sessionId, UUID membreId) {
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
        if (ordreBeneficiaireRepository.existsBySessionIdAndMembreId(sessionId, membreId)) {
            throw new IllegalStateException("Ce membre est déjà inscrit dans cette session");
        }

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
            LocalDate dateTour = obPassé.getDateBenefice() != null ? obPassé.getDateBenefice() : LocalDate.now();
            short mois  = (short) dateTour.getMonthValue();
            short annee = (short) dateTour.getYear();

            BigDecimal cotis = moyenneCotisations(tontine.getId(), mois, annee, false);
            BigDecimal repas = moyenneRepas(tontine.getId(), mois, annee);
            BigDecimal fond  = moyenneFond(tontine.getId(), mois, annee);
            if (cotis.compareTo(BigDecimal.ZERO) == 0 && tontine.getMontantCotisationMin() != null) {
                cotis = tontine.getMontantCotisationMin();
            }

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

        // Ajouter en fin de calendrier
        List<OrdreBeneficiaire> tousOrdres = ordreBeneficiaireRepository.findAllBySessionIdOrderByOrdre(sessionId);
        int prochainOrdre = tousOrdres.size() + 1;

        LocalDate derniereDateExistante = tousOrdres.stream()
                .map(OrdreBeneficiaire::getDateBenefice)
                .filter(java.util.Objects::nonNull)
                .max(java.util.Comparator.naturalOrder())
                .orElse(session.getDateDebut());

        LocalDate dateNouveauMembre = null;
        try {
            dateNouveauMembre = periodiciteService.prochainDate(
                    tontine.getTypeReglePeriodicite(), tontine.getJourReference(), derniereDateExistante);
        } catch (Exception ignored) {}

        OrdreBeneficiaire nouvelOrdre = OrdreBeneficiaire.builder()
                .session(session)
                .membre(membre)
                .ordre(prochainOrdre)
                .dateBenefice(dateNouveauMembre)
                .beneficie(false)
                .montantRecu(null)
                .build();
        ordreBeneficiaireRepository.save(nouvelOrdre);

        // Mettre à jour la session
        session.setNombreMembres(session.getNombreMembres() + 1);
        if (dateNouveauMembre != null && (session.getDateFin() == null || dateNouveauMembre.isAfter(session.getDateFin()))) {
            session.setDateFin(dateNouveauMembre);
        }
        sessionRepository.save(session);

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
