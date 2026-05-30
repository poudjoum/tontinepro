package com.tontinepro.tontinepro_backend.api.scheduler;

import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontine;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontineRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetardCotisationScheduler {

    private final SessionTontineRepository sessionRepository;
    private final CotisationRepository cotisationRepository;
    private final SanctionRepository sanctionRepository;
    private final NotificationService notificationService;

    /**
     * Chaque nuit à 00:00:01, pour chaque session EN_COURS dont la dateProchaineTontine
     * est strictement dépassée (hier ou avant), marque EN_RETARD les cotisations
     * EN_ATTENTE et applique la pénalité de retard paramétrée dans la tontine.
     */
    @Scheduled(cron = "1 0 0 * * *")
    @Transactional
    public void appliquerRetardsCotisations() {
        LocalDate today = LocalDate.now();
        log.info("[Scheduler] Vérification retards cotisations — {}", today);

        List<SessionTontine> sessionsActives =
                sessionRepository.findAllByStatut(SessionTontine.Statut.EN_COURS);

        for (SessionTontine session : sessionsActives) {
            LocalDate dateEcheance = session.getDateProchaineTontine();
            if (dateEcheance == null || !dateEcheance.isBefore(today)) {
                continue; // date non dépassée ou non renseignée
            }

            short mois  = (short) dateEcheance.getMonthValue();
            short annee = (short) dateEcheance.getYear();
            Tontine tontine = session.getTontine();

            List<Cotisation> enAttente = cotisationRepository
                    .findAllByTontineIdAndMoisAndAnneeAndStatut(
                            tontine.getId(), mois, annee, Cotisation.Statut.EN_ATTENTE);

            for (Cotisation cot : enAttente) {
                cot.setStatut(Cotisation.Statut.EN_RETARD);
                cotisationRepository.save(cot);

                BigDecimal penalite = tontine.getMontantPenaliteRetard();
                if (penalite != null && penalite.compareTo(BigDecimal.ZERO) > 0
                        && !sanctionRepository.existsByReferenceIdAndTypeSanction(
                                cot.getId(), Sanction.TypeSanction.RETARD_COTISATION)) {

                    sanctionRepository.save(Sanction.builder()
                            .membre(cot.getMembre())
                            .tontine(tontine)
                            .typeSanction(Sanction.TypeSanction.RETARD_COTISATION)
                            .montant(penalite)
                            .motif("Cotisation non payée pour %02d/%d".formatted(mois, annee))
                            .referenceId(cot.getId())
                            .build());

                    notificationService.notifier(
                            cot.getMembre().getUser(),
                            Notification.Type.SANCTION_INFLIGEE,
                            "Retard de cotisation",
                            "Votre cotisation de %02d/%d n'a pas été réglée avant l'échéance. Une pénalité de %s FCFA a été enregistrée."
                                    .formatted(mois, annee, penalite.toPlainString()),
                            cot.getId(), "COTISATION");
                }

                log.info("[Scheduler] Retard appliqué — membre {} cotisation {}/{}",
                        cot.getMembre().getId(), mois, annee);
            }
        }

        log.info("[Scheduler] Terminé — {} session(s) traitée(s)", sessionsActives.size());
    }
}
