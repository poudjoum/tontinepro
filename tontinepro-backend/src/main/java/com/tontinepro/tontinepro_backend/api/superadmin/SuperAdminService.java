package com.tontinepro.tontinepro_backend.api.superadmin;

import com.tontinepro.tontinepro_backend.api.superadmin.dto.ConfigurerRedevanceRequest;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.SuperAdminCreerTontineRequest;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.TontinePlatformeResponse;
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

        String matricule = "MBR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
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
}
