package com.tontinepro.tontinepro_backend.api.demande;

import com.tontinepro.tontinepro_backend.api.demande.dto.DemandeResponse;
import com.tontinepro.tontinepro_backend.api.demande.dto.RejeterDemandeRequest;
import com.tontinepro.tontinepro_backend.api.demande.dto.SoumettreDemandeRequest;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.demande.DemandeAdhesion;
import com.tontinepro.tontinepro_backend.domain.demande.DemandeAdhesionRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemandeService {

    private final DemandeAdhesionRepository demandeRepository;
    private final TontineRepository tontineRepository;
    private final UserRepository userRepository;
    private final MembreRepository membreRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional
    public DemandeResponse soumettre(UUID tontineId, SoumettreDemandeRequest request) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable"));

        if (!tontine.isVisible() || !tontine.isActif()) {
            throw new IllegalArgumentException("Cette tontine n'accepte pas de demandes");
        }
        if (demandeRepository.existsByEmailAndTontineId(request.email(), tontineId)) {
            throw new IllegalArgumentException("Une demande a déjà été soumise avec cet email pour cette tontine");
        }
        if (userRepository.existsByEmail(request.email())) {
            Membre membreExistant = membreRepository.findByUserEmail(request.email()).orElse(null);
            if (membreExistant != null && membreExistant.getTontine().getId().equals(tontineId)) {
                throw new IllegalArgumentException("Vous êtes déjà membre de cette tontine");
            }
        }

        DemandeAdhesion demande = DemandeAdhesion.builder()
                .tontine(tontine)
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .telephone(request.telephone())
                .motivation(request.motivation())
                .typeParticipation(request.typeParticipation() != null
                        ? request.typeParticipation() : com.tontinepro.tontinepro_backend.domain.membre.Membre.TypeParticipation.TONTINE)
                .modePaiementAide(request.modePaiementAide())
                .build();

        DemandeAdhesion saved = demandeRepository.save(demande);

        // Notifier les admins de la tontine (Secrétaire, Président)
        notifierAdminsTontine(tontine, saved);

        return DemandeResponse.from(saved);
    }

    @Transactional
    public DemandeResponse approuver(UUID demandeId, String traitePar) {
        DemandeAdhesion demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        if (demande.getStatut() != DemandeAdhesion.Statut.EN_ATTENTE) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée");
        }

        User gestionnaire = userRepository.findByEmail(traitePar).orElseThrow();

        // Générer un mot de passe provisoire
        String motDePasseProv = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        User user;
        if (userRepository.existsByEmail(demande.getEmail())) {
            user = userRepository.findByEmail(demande.getEmail()).orElseThrow();
        } else {
            user = userRepository.save(User.builder()
                    .email(demande.getEmail())
                    .hashedPassword(passwordEncoder.encode(motDePasseProv))
                    .telephone(demande.getTelephone())
                    .role(User.Role.MEMBRE)
                    .build());
        }

        Tontine tontine = demande.getTontine();
        String matricule = membreRepository.genererMatriculeUnique();

        Membre membre = membreRepository.save(Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(demande.getNom())
                .prenom(demande.getPrenom())
                .matricule(matricule)
                .typeParticipation(demande.getTypeParticipation() != null
                        ? demande.getTypeParticipation() : Membre.TypeParticipation.TONTINE)
                .modePaiementAide(demande.getModePaiementAide())
                .build());

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        demande.setStatut(DemandeAdhesion.Statut.APPROUVEE);
        demande.setTraitePar(gestionnaire);
        demandeRepository.save(demande);

        notificationService.notifier(user, Notification.Type.BIENVENUE,
                "Demande d'adhésion approuvée — " + tontine.getNom(),
                String.format(
                    "Bienvenue ! Votre demande a été approuvée. Matricule : %s. " +
                    "Mot de passe provisoire : %s (changez-le dès votre première connexion).",
                    matricule, motDePasseProv),
                null, null);

        return DemandeResponse.from(demande);
    }

    @Transactional
    public DemandeResponse rejeter(UUID demandeId, RejeterDemandeRequest request, String traitePar) {
        DemandeAdhesion demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        if (demande.getStatut() != DemandeAdhesion.Statut.EN_ATTENTE) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée");
        }

        User gestionnaire = userRepository.findByEmail(traitePar).orElseThrow();
        demande.setStatut(DemandeAdhesion.Statut.REJETEE);
        demande.setMotifRejet(request.motifRejet());
        demande.setTraitePar(gestionnaire);

        return DemandeResponse.from(demandeRepository.save(demande));
    }

    @Transactional(readOnly = true)
    public List<DemandeResponse> lister(UUID tontineId, DemandeAdhesion.Statut statut) {
        List<DemandeAdhesion> demandes;
        if (tontineId != null && statut != null) {
            demandes = demandeRepository.findAllByTontineId(tontineId).stream()
                    .filter(d -> d.getStatut() == statut).toList();
        } else if (tontineId != null) {
            demandes = demandeRepository.findAllByTontineId(tontineId);
        } else if (statut != null) {
            demandes = demandeRepository.findAllByStatut(statut);
        } else {
            demandes = demandeRepository.findAll();
        }
        return demandes.stream().map(DemandeResponse::from).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void notifierAdminsTontine(Tontine tontine, DemandeAdhesion demande) {
        List<Membre> tousLesMembres = membreRepository.findAllByTontineId(tontine.getId())
                .stream().filter(m -> m.getStatut() == Membre.Statut.ACTIF).toList();

        // Chercher Secrétaire et Président en priorité, sinon tout membre actif
        List<Membre> destinataires = tousLesMembres.stream()
                .filter(m -> m.getFonction() == Membre.Fonction.SECRETAIRE ||
                             m.getFonction() == Membre.Fonction.PRESIDENT)
                .toList();

        // Fallback : si aucun bureau, notifier le premier membre actif (créateur = Secrétaire fondateur)
        if (destinataires.isEmpty() && !tousLesMembres.isEmpty()) {
            destinataires = List.of(tousLesMembres.get(0));
        }

        String message = String.format(
                "%s %s (%s) souhaite rejoindre la tontine \"%s\". " +
                "Connectez-vous pour valider ou rejeter cette demande.",
                demande.getPrenom(), demande.getNom(), demande.getEmail(), tontine.getNom());

        for (Membre m : destinataires) {
            notificationService.notifier(m.getUser(), Notification.Type.DEMANDE_ADHESION,
                    "Nouvelle demande d'adhésion", message, demande.getId(), "DEMANDE");
        }
    }
}
