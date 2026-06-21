package com.tontinepro.tontinepro_backend.api.superadmin;

import com.tontinepro.tontinepro_backend.api.superadmin.dto.ConfigurerRedevanceRequest;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.DemandeReinitMdpResponse;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.SoumettreReinitMdpRequest;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.SuperAdminCreerTontineRequest;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.TontinePlatformeResponse;
import com.tontinepro.tontinepro_backend.domain.user.DemandeReinitMdp;
import com.tontinepro.tontinepro_backend.domain.user.DemandeReinitMdpRepository;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.redevance.Redevance;
import com.tontinepro.tontinepro_backend.domain.redevance.RedevanceRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.PasswordResetToken;
import com.tontinepro.tontinepro_backend.domain.user.PasswordResetTokenRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import com.tontinepro.tontinepro_backend.infrastructure.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final TontineRepository tontineRepository;
    private final MembreRepository membreRepository;
    private final RedevanceRepository redevanceRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final DemandeReinitMdpRepository demandeReinitRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompteEpargneRepository compteEpargneRepository;
    private final FondsAideRepository fondsAideRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public TontinePlatformeResponse creerTontine(SuperAdminCreerTontineRequest req) {
        if (tontineRepository.existsByNom(req.tontineNom())) {
            throw new IllegalArgumentException("Une tontine avec ce nom existe déjà");
        }
        if (userRepository.existsByEmail(req.secretaireEmail())) {
            throw new IllegalArgumentException("Un compte existe déjà avec l'email : " + req.secretaireEmail());
        }

        Tontine tontine = tontineRepository.save(Tontine.builder()
                .nom(req.tontineNom())
                .description(req.tontineDescription())
                .montantCotisationMin(req.montantCotisationMin())
                .typeAcces(req.typeAcces() != null ? req.typeAcces() : Tontine.TypeAcces.OUVERTE)
                .descriptionAcces(req.descriptionAcces())
                .build());

        fondsAideRepository.save(FondsAide.builder().tontine(tontine).build());

        User secretaire = userRepository.save(User.builder()
                .email(req.secretaireEmail())
                .hashedPassword(passwordEncoder.encode(req.secretairePassword()))
                .telephone(req.secretaireTelephone())
                .role(User.Role.SECRETAIRE)
                .build());

        String matricule = membreRepository.genererMatriculeUnique();
        Membre membre = membreRepository.save(Membre.builder()
                .user(secretaire)
                .tontine(tontine)
                .nom(req.secretaireNom())
                .prenom(req.secretairePrenom())
                .matricule(matricule)
                .fonction(Membre.Fonction.SECRETAIRE)
                .build());

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return toResponse(tontine);
    }

    @Transactional(readOnly = true)
    public List<TontinePlatformeResponse> listerTontines() {
        return tontineRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TontinePlatformeResponse toggleActif(UUID tontineId, boolean actif) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + tontineId));
        tontine.setActif(actif);
        return toResponse(tontineRepository.save(tontine));
    }

    @Transactional
    public TontinePlatformeResponse configurerRedevance(UUID tontineId, ConfigurerRedevanceRequest req) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + tontineId));

        Redevance redevance = redevanceRepository.findByTontineId(tontineId)
                .orElseGet(() -> Redevance.builder().tontine(tontine).build());

        redevance.setMontant(req.montant());
        redevance.setPeriodicite(req.periodicite());
        redevance.setStatut(req.statut());
        redevance.setProchainPaiement(req.prochainPaiement());
        redevanceRepository.save(redevance);

        return toResponse(tontine);
    }

    /**
     * Génère un token de réinitialisation de mot de passe pour le Président et le Secrétaire
     * de la tontine, et leur envoie le lien par email.
     */
    @Transactional
    public int reinitialiserMotDePasse(UUID tontineId) {
        List<Membre> bureau = membreRepository.findAllByTontineIdAndStatut(tontineId, Membre.Statut.ACTIF)
                .stream()
                .filter(m -> m.getFonction() == Membre.Fonction.PRESIDENT
                          || m.getFonction() == Membre.Fonction.SECRETAIRE)
                .toList();

        if (bureau.isEmpty()) {
            throw new IllegalStateException(
                    "Aucun Président ni Secrétaire actif trouvé pour cette tontine");
        }

        int envois = 0;
        for (Membre m : bureau) {
            // Invalider les anciens tokens de cet utilisateur
            resetTokenRepository.deleteAllByUserId(m.getUser().getId());

            String token = UUID.randomUUID().toString();
            resetTokenRepository.save(PasswordResetToken.builder()
                    .user(m.getUser())
                    .token(token)
                    .expiresAt(OffsetDateTime.now().plusHours(24))
                    .build());

            String lien = frontendUrl + "/auth/reset-password?token=" + token;
            String fonctionLabel = m.getFonction() == Membre.Fonction.PRESIDENT ? "Président" : "Secrétaire";
            emailService.envoyer(
                    m.getUser().getEmail(),
                    "Réinitialisation de votre mot de passe TontinePro",
                    String.format(
                        "Bonjour %s %s (%s de la tontine \"%s\"),\n\n" +
                        "Le super-administrateur de la plateforme TontinePro a déclenché une " +
                        "réinitialisation de votre mot de passe.\n\n" +
                        "Cliquez sur le lien suivant pour choisir un nouveau mot de passe " +
                        "(valable 24 heures) :\n\n%s\n\n" +
                        "Si vous n'attendiez pas cette demande, contactez le support.",
                        m.getPrenom(), m.getNom(), fonctionLabel,
                        m.getTontine().getNom(), lien)
            );
            envois++;
        }
        return envois;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TontinePlatformeResponse toResponse(Tontine t) {
        List<Membre> membres = membreRepository.findAllByTontineIdAndStatut(
                t.getId(), Membre.Statut.ACTIF);

        Membre president = membres.stream()
                .filter(m -> m.getFonction() == Membre.Fonction.PRESIDENT)
                .findFirst().orElse(null);

        Membre secretaire = membres.stream()
                .filter(m -> m.getFonction() == Membre.Fonction.SECRETAIRE)
                .findFirst().orElse(null);

        TontinePlatformeResponse.RedevanceInfo rev = redevanceRepository.findByTontineId(t.getId())
                .map(r -> new TontinePlatformeResponse.RedevanceInfo(
                        r.getId(), r.getMontant(),
                        r.getPeriodicite().name(), r.getStatut().name(),
                        r.getProchainPaiement()))
                .orElse(null);

        return new TontinePlatformeResponse(
                t.getId(), t.getNom(), t.getDescription(), t.isActif(),
                membres.size(),
                president != null ? president.getUser().getEmail() : null,
                president != null ? president.getUser().getTelephone() : null,
                president != null ? president.getNom() : null,
                president != null ? president.getPrenom() : null,
                secretaire != null ? secretaire.getUser().getEmail() : null,
                secretaire != null ? secretaire.getUser().getTelephone() : null,
                secretaire != null ? secretaire.getNom() : null,
                secretaire != null ? secretaire.getPrenom() : null,
                rev);
    }

    // ── Réinitialisation mot de passe à la demande ──────────────────────────────

    @Transactional
    public DemandeReinitMdpResponse soumettreDemande(SoumettreReinitMdpRequest request) {
        DemandeReinitMdp demande = DemandeReinitMdp.builder()
                .email(request.email().toLowerCase().trim())
                .nom(request.nom().trim())
                .prenom(request.prenom().trim())
                .telephone(request.telephone())
                .build();
        return DemandeReinitMdpResponse.from(demandeReinitRepository.save(demande));
    }

    @Transactional(readOnly = true)
    public List<DemandeReinitMdpResponse> listerDemandesReinit() {
        return demandeReinitRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(DemandeReinitMdpResponse::from).toList();
    }

    @Transactional
    public void envoyerLienReinit(UUID demandeId) {
        DemandeReinitMdp demande = demandeReinitRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + demandeId));

        // Chercher le compte associé à cet email (pas obligatoire — on envoie quand même)
        User user = userRepository.findByEmail(demande.getEmail()).orElse(null);
        if (user == null) {
            demande.setStatut(DemandeReinitMdp.Statut.ENVOYEE);
            demandeReinitRepository.save(demande);
            emailService.envoyer(
                    demande.getEmail(),
                    "Demande de réinitialisation — TontinePro",
                    String.format(
                        "Bonjour %s %s,\n\n" +
                        "Nous n'avons trouvé aucun compte associé à l'adresse %s.\n\n" +
                        "Si vous pensez qu'il s'agit d'une erreur, contactez votre administrateur de tontine.\n\n" +
                        "Cordialement,\nL'équipe TontinePro",
                        demande.getPrenom(), demande.getNom(), demande.getEmail()));
            return;
        }

        // Invalider les anciens tokens
        resetTokenRepository.deleteAllByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build());

        String lien = frontendUrl + "/auth/reset-password?token=" + token;
        emailService.envoyer(
                demande.getEmail(),
                "Réinitialisation de votre mot de passe TontinePro",
                String.format(
                    "Bonjour %s %s,\n\n" +
                    "Vous avez demandé la réinitialisation de votre mot de passe.\n\n" +
                    "Cliquez sur le lien ci-dessous pour définir un nouveau mot de passe :\n%s\n\n" +
                    "Ce lien est valable 24 heures.\n\n" +
                    "Si vous n'êtes pas à l'origine de cette demande, ignorez ce message.\n\n" +
                    "Cordialement,\nL'équipe TontinePro",
                    demande.getPrenom(), demande.getNom(), lien));

        demande.setStatut(DemandeReinitMdp.Statut.ENVOYEE);
        demandeReinitRepository.save(demande);
    }
}
