package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.rapport.builder.RapportTourPdfBuilder;
import com.tontinepro.tontinepro_backend.api.session.dto.RapportTourResponse;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class RapportEmailService {

    private final MembreRepository membreRepository;
    private final RapportTourPdfBuilder pdfBuilder;
    private final JavaMailSender mailSender;

    public RapportEmailService(MembreRepository membreRepository, RapportTourPdfBuilder pdfBuilder,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender) {
        this.membreRepository = membreRepository;
        this.pdfBuilder = pdfBuilder;
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.from:noreply@tontinepro.com}")
    private String from;

    private static final String[] MOIS_FR = {"", "Janvier", "Février", "Mars", "Avril", "Mai",
        "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Async("notificationExecutor")
    public void envoyerRapportTour(UUID tontineId, RapportTourResponse rapport) {
        List<Membre> membres = membreRepository.findAllByTontineId(tontineId);
        if (membres.isEmpty()) return;

        byte[] pdf;
        try {
            pdf = pdfBuilder.build(rapport);
        } catch (Exception e) {
            log.error("Erreur génération PDF rapport de tour : {}", e.getMessage());
            return;
        }

        String nomFichier = "rapport-tour-session" + rapport.sessionNumero()
                + "-" + MOIS_FR[rapport.mois()] + rapport.annee() + ".pdf";

        String sujet = "[TontinePro] Rapport de tour — " + rapport.tontineNom()
                + " — " + MOIS_FR[rapport.mois()] + " " + rapport.annee();

        String corps = buildCorps(rapport);

        for (Membre m : membres) {
            String email = m.getUser().getEmail();
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(email);
                helper.setSubject(sujet);
                helper.setText(corps, false);
                helper.addAttachment(nomFichier, new org.springframework.core.io.ByteArrayResource(pdf));
                mailSender.send(msg);
                log.debug("Rapport de tour envoyé à {}", email);
            } catch (MessagingException e) {
                log.error("Échec envoi rapport à {} : {}", email, e.getMessage());
            }
        }
    }

    private String buildCorps(RapportTourResponse r) {
        String date = r.dateTour() != null ? r.dateTour().format(FMT) : "—";
        return """
                Bonjour,

                Le rapport du tour de tontine est disponible en pièce jointe.

                Tontine     : %s
                Session     : n°%d
                Mois        : %s %d
                Date du tour: %s
                Bénéficiaire: %s (%s)
                Cagnotte    : %,.0f FCFA

                Veuillez trouver le rapport complet en pièce jointe (PDF).

                --
                TontinePro
                """.formatted(r.tontineNom(), r.sessionNumero(),
                MOIS_FR[r.mois()], r.annee(), date,
                r.beneficiaireNom(), r.beneficiaireMatricule(),
                r.cagnotteBeneficiaire());
    }
}
