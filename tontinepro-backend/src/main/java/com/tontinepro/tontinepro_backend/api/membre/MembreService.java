package com.tontinepro.tontinepro_backend.api.membre;

import com.tontinepro.tontinepro_backend.api.membre.dto.CreateMembreRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.InscriptionDirecteRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.MembreResponse;
import com.tontinepro.tontinepro_backend.api.membre.dto.ReinitialiserMembresResponse;
import com.tontinepro.tontinepro_backend.api.membre.dto.UpdateMembreFonctionRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.UpdateMembreStatutRequest;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembreService {

    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final TontineRepository tontineRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final CotisationRepository cotisationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MembreResponse create(CreateMembreRequest request) {
        if (membreRepository.existsByUserIdAndTontineId(request.userId(), request.tontineId())) {
            throw new IllegalArgumentException("Cet utilisateur est déjà membre de cette tontine");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + request.userId()));

        Tontine tontine = tontineRepository.findById(request.tontineId())
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + request.tontineId()));

        if (!tontine.isActif()) {
            throw new IllegalArgumentException("Impossible d'adhérer à une tontine inactive");
        }

        Membre membre = Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(request.nom())
                .prenom(request.prenom())
                .matricule(generateMatricule())
                .dateAdhesion(request.dateAdhesion() != null ? request.dateAdhesion() : LocalDate.now())
                .fonction(request.fonction() != null ? request.fonction() : Membre.Fonction.MEMBRE_ORDINAIRE)
                .typeParticipation(request.typeParticipation() != null
                        ? request.typeParticipation() : Membre.TypeParticipation.TONTINE)
                .modePaiementAide(request.modePaiementAide())
                .build();

        membre = membreRepository.save(membre);
        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return MembreResponse.from(membre);
    }

    @Transactional(readOnly = true)
    public MembreResponse getById(UUID id) {
        return membreRepository.findById(id)
                .map(MembreResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public MembreResponse getMe(String email) {
        return getMe(email, null);
    }

    /** Profil du compte dans la tontine donnée (ou 1ᵉʳ profil si tontineId null). */
    @Transactional(readOnly = true)
    public MembreResponse getMe(String email, UUID tontineId) {
        var membre = tontineId != null
                ? membreRepository.findByUserEmailAndTontineId(email, tontineId)
                : membreRepository.findByUserEmail(email);
        return membre
                .map(MembreResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte pour cette tontine"));
    }

    @Transactional(readOnly = true)
    public List<MembreResponse> list(UUID tontineId, Membre.Statut statut, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        boolean superAdmin = requester.getRole() == User.Role.SUPER_ADMIN;

        // Cloisonnement multi-tontine : hors super-admin, on ne peut lister que les
        // membres d'une tontine où l'on a soi-même un profil membre actif.
        if (!superAdmin) {
            if (tontineId == null) {
                throw new AccessDeniedException("Une tontine doit être précisée");
            }
            boolean membreDeLaTontine = membreRepository
                    .findByUserEmailAndTontineId(requesterEmail, tontineId)
                    .filter(m -> m.getStatut() == Membre.Statut.ACTIF)
                    .isPresent();
            if (!membreDeLaTontine) {
                throw new AccessDeniedException("Vous n'appartenez pas à cette tontine");
            }
        }

        List<Membre> membres;
        if (tontineId != null && statut != null) {
            membres = membreRepository.findAllByTontineIdAndStatut(tontineId, statut);
        } else if (tontineId != null) {
            membres = membreRepository.findAllByTontineId(tontineId);
        } else {
            membres = membreRepository.findAll();
        }

        return membres.stream().map(MembreResponse::from).toList();
    }

    @Transactional
    public MembreResponse updateStatut(UUID id, UpdateMembreStatutRequest request) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + id));

        membre.setStatut(request.statut());
        return MembreResponse.from(membreRepository.save(membre));
    }

    @Transactional
    public MembreResponse updateFonction(UUID id, UpdateMembreFonctionRequest request, String auteurEmail) {
        Membre membre = membreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + id));

        verifierQueLAuteurNeSeRetirePas(membre, request.fonction(), auteurEmail);
        verifierQueLeBureauResteAdministrable(membre, request.fonction());

        membre.setFonction(request.fonction());
        membreRepository.save(membre);

        synchroniserRole(membre.getUser());
        return MembreResponse.from(membre);
    }

    /**
     * Personne ne se retire à soi-même sa fonction dirigeante. Le rôle applicatif
     * en découle, et c'est ce rôle qui autorise à modifier les fonctions : l'auteur
     * du changement perdrait donc, du même geste, le droit de le défaire. Qu'un
     * autre dirigeant existe ne suffit pas — encore faudrait-il disposer de son
     * compte. Un SUPER_ADMIN n'est pas concerné : son rôle ne dépend d'aucune
     * fonction et ne lui sera pas retiré.
     */
    private void verifierQueLAuteurNeSeRetirePas(Membre membre, Membre.Fonction nouvelle, String auteurEmail) {
        User user = membre.getUser();
        if (user == null || user.getRole() == User.Role.SUPER_ADMIN) return;
        if (!user.getEmail().equalsIgnoreCase(auteurEmail)) return;
        if (estDirigeante(membre.getFonction()) && !estDirigeante(nouvelle)) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez pas vous retirer vous-même votre fonction de "
                    + "Président ou Secrétaire : vous perdriez l'accès à l'administration "
                    + "sans pouvoir revenir en arrière. Demandez-le à un autre dirigeant.");
        }
    }

    /**
     * Une tontine doit toujours conserver au moins un Président ou un Secrétaire
     * actif : ce sont les seules fonctions qui donnent le rôle applicatif
     * permettant de modifier les fonctions du bureau. Lui retirer son dernier
     * dirigeant la rendrait définitivement inadministrable — plus personne ne
     * pourrait défaire le changement.
     */
    private void verifierQueLeBureauResteAdministrable(Membre membre, Membre.Fonction nouvelle) {
        if (estDirigeante(nouvelle) || !estDirigeante(membre.getFonction())) {
            return; // on n'enlève pas de dirigeant, rien à vérifier
        }
        boolean autreDirigeant = membreRepository
                .findAllByTontineIdAndStatut(membre.getTontine().getId(), Membre.Statut.ACTIF)
                .stream()
                .anyMatch(m -> !m.getId().equals(membre.getId()) && estDirigeante(m.getFonction()));
        if (!autreDirigeant) {
            throw new IllegalArgumentException(
                    "Impossible : la tontine n'aurait plus aucun Président ni Secrétaire pour "
                    + "l'administrer. Désignez d'abord un autre dirigeant.");
        }
    }

    private static boolean estDirigeante(Membre.Fonction f) {
        return f == Membre.Fonction.PRESIDENT || f == Membre.Fonction.SECRETAIRE;
    }

    /**
     * Recalcule le rôle applicatif d'un compte à partir de <em>toutes</em> ses
     * adhésions.
     *
     * <p>Le rôle est global au compte alors que la fonction est propre à une
     * tontine : le déduire de la seule tontine qu'on vient de modifier faisait
     * perdre à un président de la tontine A son rôle ADMIN parce qu'on l'avait
     * nommé trésorier dans la tontine B. On retient donc la fonction la plus
     * élevée détenue, toutes tontines confondues.</p>
     *
     * <p>Un SUPER_ADMIN n'est jamais touché : son rôle est celui de l'opérateur
     * de la plateforme, il ne découle d'aucune adhésion. L'ancienne version le
     * rétrogradait en MEMBRE dès qu'on lui attribuait une fonction dans une
     * tontine.</p>
     */
    private void synchroniserRole(User user) {
        if (user.getRole() == User.Role.SUPER_ADMIN) return;

        List<Membre> adhesions = membreRepository.findAllByUserEmail(user.getEmail());
        boolean president  = adhesions.stream()
                .anyMatch(m -> m.getFonction() == Membre.Fonction.PRESIDENT);
        boolean secretaire = adhesions.stream()
                .anyMatch(m -> m.getFonction() == Membre.Fonction.SECRETAIRE);

        User.Role role = president ? User.Role.ADMIN
                : secretaire ? User.Role.SECRETAIRE
                : User.Role.MEMBRE;

        if (user.getRole() != role) {
            user.setRole(role);
            userRepository.save(user);
        }
    }

    @Transactional
    public MembreResponse inscrireDirectement(InscriptionDirecteRequest request) {
        Tontine tontine = tontineRepository.findById(request.tontineId())
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + request.tontineId()));

        if (!tontine.isActif()) {
            throw new IllegalArgumentException("Impossible d'inscrire dans une tontine inactive");
        }

        // Autoriser si l'utilisateur existe déjà — il peut rejoindre une autre tontine
        // Mais il ne peut pas être inscrit deux fois dans la même tontine
        userRepository.findByEmail(request.email()).ifPresent(existant -> {
            if (membreRepository.existsByUserIdAndTontineId(existant.getId(), tontine.getId())) {
                throw new IllegalArgumentException("Cet utilisateur est déjà membre de cette tontine");
            }
        });

        Membre.Fonction fonction = request.fonction() != null
                ? request.fonction() : Membre.Fonction.MEMBRE_ORDINAIRE;

        // Le Président reçoit le rôle ADMIN, le Secrétaire le rôle SECRETAIRE
        User.Role role = switch (fonction) {
            case PRESIDENT  -> User.Role.ADMIN;
            case SECRETAIRE -> User.Role.SECRETAIRE;
            default         -> User.Role.MEMBRE;
        };

        // Réutiliser le compte existant s'il y en a un, sinon en créer un nouveau
        User user = userRepository.findByEmail(request.email()).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(request.email())
                        .hashedPassword(passwordEncoder.encode(request.password()))
                        .telephone(request.telephone())
                        .role(role)
                        .build())
        );

        Membre membre = Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(request.nom())
                .prenom(request.prenom())
                .matricule(generateMatricule())
                .fonction(fonction)
                .typeParticipation(request.typeParticipation() != null
                        ? request.typeParticipation() : Membre.TypeParticipation.TONTINE)
                .modePaiementAide(request.modePaiementAide())
                .build();
        membre = membreRepository.save(membre);
        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return MembreResponse.from(membre);
    }

    /**
     * Supprime les membres d'une tontine qui n'ont aucune activité financière
     * pour qu'ils puissent se réinscrire proprement via un lien d'invitation.
     * Les membres ayant des cotisations sont ignorés (données financières préservées).
     * Le compte User est supprimé uniquement si le membre n'appartient à aucune autre tontine.
     */
    @Transactional
    public ReinitialiserMembresResponse reinitialiserPourInvitation(UUID tontineId) {
        List<Membre> membres = membreRepository.findAllByTontineId(tontineId);

        List<ReinitialiserMembresResponse.MembreInfo> supprimes = new ArrayList<>();
        List<ReinitialiserMembresResponse.MembreInfo> ignores  = new ArrayList<>();

        for (Membre m : membres) {
            boolean aCotisations = !cotisationRepository.findAllByMembreId(m.getId()).isEmpty();
            String tel = m.getUser().getTelephone();

            if (aCotisations) {
                ignores.add(new ReinitialiserMembresResponse.MembreInfo(
                        m.getNom(), m.getPrenom(), tel, "cotisations existantes"));
                continue;
            }

            User user = m.getUser();
            compteEpargneRepository.findByMembreId(m.getId())
                    .ifPresent(compteEpargneRepository::delete);
            membreRepository.delete(m);

            // Supprimer le compte utilisateur s'il n'a plus aucun autre profil membre
            if (!membreRepository.existsByUserId(user.getId())) {
                userRepository.delete(user);
            }

            supprimes.add(new ReinitialiserMembresResponse.MembreInfo(
                    m.getNom(), m.getPrenom(), tel, null));
        }

        return new ReinitialiserMembresResponse(supprimes, ignores);
    }

    private String generateMatricule() {
        return membreRepository.genererMatriculeUnique();
    }
}
