package com.tontinepro.tontinepro_backend.api.dashboard;

import com.tontinepro.tontinepro_backend.api.dashboard.dto.AdminDashboardResponse;
import com.tontinepro.tontinepro_backend.api.dashboard.dto.MembreDashboardResponse;
import com.tontinepro.tontinepro_backend.domain.aide.AideRepository;
import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.notification.NotificationRepository;
import com.tontinepro.tontinepro_backend.domain.pret.EcheancePret;
import com.tontinepro.tontinepro_backend.domain.pret.EcheancePretRepository;
import com.tontinepro.tontinepro_backend.domain.pret.Pret;
import com.tontinepro.tontinepro_backend.domain.pret.PretRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MembreRepository        membreRepository;
    private final CotisationRepository    cotisationRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final PretRepository          pretRepository;
    private final EcheancePretRepository  echeancePretRepository;
    private final AideRepository          aideRepository;
    private final FondsAideRepository     fondsAideRepository;
    private final NotificationRepository  notificationRepository;
    private final UserRepository          userRepository;
    private final TontineRepository       tontineRepository;

    // ── Tableau de bord Membre ────────────────────────────────────────

    @Transactional(readOnly = true)
    public MembreDashboardResponse getMembreDashboard(String email) {
        Membre membre = membreRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre pour ce compte"));

        // Épargne
        BigDecimal soldeEpargne = compteEpargneRepository.findByMembreId(membre.getId())
                .map(CompteEpargne::getSolde)
                .orElse(BigDecimal.ZERO);

        // Dernière cotisation connue
        List<Cotisation> cotisations = cotisationRepository.findAllByMembreId(membre.getId());
        Cotisation derniere = cotisations.stream()
                .max(Comparator.comparingInt((Cotisation c) -> c.getAnnee() * 12 + c.getMois())
                )
                .orElse(null);

        // Prêt actif
        Optional<Pret> pretActif = pretRepository.findAllByMembreIdOrderByCreatedAtDesc(membre.getId())
                .stream()
                .filter(p -> p.getStatut() == Pret.Statut.EN_COURS)
                .findFirst();

        long echeancesRestantes = 0;
        if (pretActif.isPresent()) {
            echeancesRestantes = echeancePretRepository.countByPretIdAndStatutNot(
                    pretActif.get().getId(), EcheancePret.Statut.PAYEE);
        }

        // Notifications
        long notifNonLues = notificationRepository.countByUserIdAndLuFalse(membre.getUser().getId());

        return new MembreDashboardResponse(
                membre.getMatricule(),
                membre.getNom(),
                membre.getPrenom(),
                membre.getFonction(),
                membre.getStatut(),
                membre.getTontine().getNom(),
                soldeEpargne,
                derniere != null ? derniere.getMois()   : null,
                derniere != null ? derniere.getAnnee()  : null,
                membre.getTontine().getMontantCotisation(),
                derniere != null ? derniere.getStatut().name() : null,
                pretActif.map(Pret::getId).orElse(null),
                pretActif.map(Pret::getMontantPrincipal).orElse(null),
                pretActif.map(Pret::getMontantTotal).orElse(null),
                pretActif.map(Pret::getDureeMois).orElse((short) 0),
                echeancesRestantes,
                notifNonLues
        );
    }

    // ── Tableau de bord Admin ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard(String email) {
        // Tontine de l'admin via son profil membre, sinon première tontine active
        UUID tontineId = membreRepository.findByUserEmail(email)
                .map(m -> m.getTontine().getId())
                .orElseGet(() -> tontineRepository.findAllByActifTrue()
                        .stream().findFirst()
                        .map(Tontine::getId)
                        .orElseThrow(() -> new IllegalStateException("Aucune tontine configurée"))
                );

        Tontine tontine = tontineRepository.findById(tontineId).orElseThrow();

        // Membres
        List<Membre> tousLesMembres = membreRepository.findAllByTontineId(tontineId);
        long membresActifs = tousLesMembres.stream()
                .filter(m -> m.getStatut() == Membre.Statut.ACTIF).count();

        // Cotisations du mois en cours
        short mois  = (short) LocalDate.now().getMonthValue();
        short annee = (short) LocalDate.now().getYear();
        List<Cotisation> cotisationsMois =
                cotisationRepository.findAllByTontineIdAndMoisAndAnnee(tontineId, mois, annee);

        long payees   = cotisationsMois.stream().filter(c -> c.getStatut() == Cotisation.Statut.PAYEE).count();
        long enRetard = cotisationsMois.stream().filter(c -> c.getStatut() == Cotisation.Statut.EN_RETARD).count();
        long enAttente= cotisationsMois.size() - payees - enRetard;

        BigDecimal montantCollecte = cotisationsMois.stream()
                .filter(c -> c.getStatut() == Cotisation.Statut.PAYEE)
                .map(Cotisation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montantAttendu = tontine.getMontantCotisation()
                .multiply(BigDecimal.valueOf(membresActifs));

        // Épargne totale
        BigDecimal totalEpargne = compteEpargneRepository.findAllByMembreTontineId(tontineId)
                .stream().map(CompteEpargne::getSolde).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fonds d'aide
        BigDecimal fondsAideSolde = fondsAideRepository.findByTontineId(tontineId)
                .map(f -> f.getSolde()).orElse(BigDecimal.ZERO);

        // Prêts
        List<Pret> tousLesPrets = pretRepository.findAllByMembreTontineId(tontineId);
        List<Pret> pretsEnCours = tousLesPrets.stream()
                .filter(p -> p.getStatut() == Pret.Statut.EN_COURS).toList();
        BigDecimal encoursPrets = pretsEnCours.stream()
                .map(Pret::getMontantTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Alertes
        long aidesEnAttente = aideRepository.findAllByMembreTontineId(tontineId)
                .stream().filter(a -> a.getStatut() == Aide.Statut.SOUMISE).count();
        long pretsEnAttente = tousLesPrets.stream()
                .filter(p -> p.getStatut() == Pret.Statut.DEMANDE).count();

        return new AdminDashboardResponse(
                tontineId, tontine.getNom(),
                membresActifs, tousLesMembres.size(),
                payees, enRetard, enAttente, montantCollecte, montantAttendu,
                totalEpargne, fondsAideSolde,
                pretsEnCours.size(), encoursPrets,
                aidesEnAttente, pretsEnAttente
        );
    }
}
