package com.tontinepro.tontinepro_backend.api.notification;

import com.tontinepro.tontinepro_backend.api.notification.dto.NotificationResponse;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.notification.NotificationRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import com.tontinepro.tontinepro_backend.infrastructure.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository         userRepository;
    private final EmailService           emailService;

    // ── Création ─────────────────────────────────────────────────────────

    /**
     * Crée une notification en base et déclenche l'envoi d'email asynchrone.
     */
    @Transactional
    public void notifier(User destinataire, Notification.Type type,
                         String titre, String message,
                         UUID referenceId, String referenceType) {

        notificationRepository.save(Notification.builder()
                .user(destinataire)
                .type(type)
                .titre(titre)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build());

        emailService.envoyer(destinataire.getEmail(), titre, message);
    }

    /**
     * Variante de {@link #notifier} quand l'email doit être plus détaillé que la
     * notification affichée dans l'application (lien de consultation, rappel des
     * montants…). La notification in-app reste courte, l'email porte le détail.
     */
    @Transactional
    public void notifier(User destinataire, Notification.Type type,
                         String titre, String message,
                         String sujetEmail, String corpsEmail,
                         UUID referenceId, String referenceType) {

        notificationRepository.save(Notification.builder()
                .user(destinataire)
                .type(type)
                .titre(titre)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build());

        emailService.envoyer(destinataire.getEmail(), sujetEmail, corpsEmail);
    }

    /** Notifie tous les administrateurs. */
    @Transactional
    public void notifierAdmins(Notification.Type type, String titre, String message,
                                UUID referenceId, String referenceType) {
        userRepository.findAllByRole(User.Role.ADMIN)
                .forEach(admin -> notifier(admin, type, titre, message, referenceId, referenceType));
    }

    // ── Lecture ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMesNotifications(String email) {
        return userRepository.findByEmail(email)
                .map(u -> notificationRepository.findAllByUserIdOrderByCreatedAtDesc(u.getId())
                        .stream().map(NotificationResponse::from).toList())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    @Transactional(readOnly = true)
    public long countNonLues(String email) {
        return userRepository.findByEmail(email)
                .map(u -> notificationRepository.countByUserIdAndLuFalse(u.getId()))
                .orElse(0L);
    }

    @Transactional
    public NotificationResponse marquerLue(UUID notifId, String email) {
        Notification notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable : " + notifId));

        if (!notif.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("Accès non autorisé à cette notification");
        }

        notif.setLu(true);
        return NotificationResponse.from(notificationRepository.save(notif));
    }

    @Transactional
    public void marquerToutesLues(String email) {
        userRepository.findByEmail(email)
                .ifPresent(u -> notificationRepository.marquerToutesLues(u.getId()));
    }
}
