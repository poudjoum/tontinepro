package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.session.dto.*;
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
