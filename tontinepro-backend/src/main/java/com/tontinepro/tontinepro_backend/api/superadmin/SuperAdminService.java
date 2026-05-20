package com.tontinepro.tontinepro_backend.api.superadmin;

import com.tontinepro.tontinepro_backend.api.superadmin.dto.ConfigurerRedevanceRequest;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.TontinePlatformeResponse;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.redevance.Redevance;
import com.tontinepro.tontinepro_backend.domain.redevance.RedevanceRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.PasswordResetToken;
import com.tontinepro.tontinepro_backend.domain.user.PasswordResetTokenRepository;
import com.tontinepro.tontinepro_backend.infrastructure.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

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

        String emailPresident = membres.stream()
                .filter(m -> m.getFonction() == Membre.Fonction.PRESIDENT)
                .map(m -> m.getUser().getEmail())
                .findFirst().orElse(null);

        String emailSecretaire = membres.stream()
                .filter(m -> m.getFonction() == Membre.Fonction.SECRETAIRE)
                .map(m -> m.getUser().getEmail())
                .findFirst().orElse(null);

        TontinePlatformeResponse.RedevanceInfo rev = redevanceRepository.findByTontineId(t.getId())
                .map(r -> new TontinePlatformeResponse.RedevanceInfo(
                        r.getId(), r.getMontant(),
                        r.getPeriodicite().name(), r.getStatut().name(),
                        r.getProchainPaiement()))
                .orElse(null);

        return new TontinePlatformeResponse(
                t.getId(), t.getNom(), t.getDescription(), t.isActif(),
                membres.size(), emailPresident, emailSecretaire, rev);
    }
}
