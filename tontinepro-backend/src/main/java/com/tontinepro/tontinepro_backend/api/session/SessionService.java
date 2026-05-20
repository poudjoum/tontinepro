package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.cotisation.dto.CotisationResponse;
import com.tontinepro.tontinepro_backend.api.session.dto.*;
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
    private final PeriodiciteService periodiciteService;

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
        return sessionRepository.findAllByTontineIdOrderByNumeroDesc(tontineId).stream()
                .map(s -> {
                    List<OrdreBeneficiaireResponse> beneficiaires = ordreBeneficiaireRepository
                            .findAllBySessionIdOrderByOrdre(s.getId()).stream()
                            .map(OrdreBeneficiaireResponse::from)
                            .toList();
                    return SessionResponse.from(s, beneficiaires);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionResponse getById(UUID id) {
        SessionTontine session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + id));
        List<OrdreBeneficiaireResponse> beneficiaires = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(id).stream()
                .map(OrdreBeneficiaireResponse::from)
                .toList();
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

        return getById(sessionId);
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

        // Mettre à jour dateProchaineTontine si c'est le prochain bénéficiaire
        SessionTontine session = ob.getSession();
        List<OrdreBeneficiaire> tous = ordreBeneficiaireRepository
                .findAllBySessionIdOrderByOrdre(sessionId);
        tous.stream()
                .filter(o -> !o.isBeneficie())
                .min(Comparator.comparingInt(OrdreBeneficiaire::getOrdre))
                .ifPresent(prochain -> {
                    if (prochain.getId().equals(ordreBeneficiaireId)) {
                        session.setDateProchaineTontine(request.dateBenefice());
                        sessionRepository.save(session);
                    }
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
                .stream().map(OrdreBeneficiaireResponse::from).toList();
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
                Cotisation c = cotisationRepository.save(Cotisation.builder()
                        .membre(membre)
                        .tontine(tontine)
                        .mois(mois)
                        .annee(annee)
                        .montant(montant)
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
        return new SessionCotisationsStatutResponse(
                sessionId, session.getNumero(), mois, annee,
                total, nbPayes, nbEnAttente, nbEnRetard, nbAbsents,
                nbPayes == total && total > 0,
                membres
        );
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
