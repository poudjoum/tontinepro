package com.tontinepro.tontinepro_backend.infrastructure.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    /** Null si spring.mail.host n'est pas configuré. */
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@tontinepro.com}")
    private String from;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envoie un email de manière asynchrone.
     * Un échec n'est jamais propagé — il est uniquement journalisé.
     */
    @Async("notificationExecutor")
    public void envoyer(String destinataire, String sujet, String corps) {
        if (mailSender == null) {
            log.warn("Email non configuré — ignoré. À: {} | Sujet: {}", destinataire, sujet);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(destinataire);
            msg.setSubject("[TontinePro] " + sujet);
            msg.setText(corps);
            mailSender.send(msg);
            log.debug("Email envoyé à {}", destinataire);
        } catch (MailException e) {
            log.error("Échec envoi email à {} — {}", destinataire, e.getMessage());
        }
    }
}
