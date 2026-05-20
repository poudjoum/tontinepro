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
                .build();

        return DemandeResponse.from(demandeRepository.save(demande));
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
        String matricule = "MBR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        Membre membre = membreRepository.save(Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(demande.getNom())
                .prenom(demande.getPrenom())
                .matricule(matricule)
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
}
