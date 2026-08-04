package com.tontinepro.tontinepro_backend.api.scheduler;

import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.AideRepository;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Alerte sur les aides dont le délai de recouvrement est dépassé.
 *
 * <p>Une aide activée doit être entièrement collectée en 3 séances de tontine.
 * Passé l'échéance, ce job signale une seule fois le retard au bureau de la
 * tontine concernée et à chaque membre qui n'a pas versé sa part. Aucune sanction
 * n'est appliquée automatiquement : le bureau garde la main.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecouvrementAideScheduler {

    private static final Set<Membre.Fonction> BUREAU = EnumSet.of(
            Membre.Fonction.PRESIDENT, Membre.Fonction.SECRETAIRE, Membre.Fonction.TRESORIER);

    private final AideRepository aideRepository;
    private final ContributionFondsAideRepository contributionRepository;
    private final MembreRepository membreRepository;
    private final NotificationService notificationService;

    /** Chaque nuit à 00:10. */
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void alerterAidesNonRecouvrees() {
        LocalDate today = LocalDate.now();
        List<Aide> echues = aideRepository
                .findAllByRelanceRecouvrementEnvoyeeFalseAndDateEcheanceRecouvrementLessThan(today);

        int alertees = 0;
        for (Aide aide : echues) {
            if (aide.getStatut() != Aide.Statut.VALIDEE && aide.getStatut() != Aide.Statut.PAYEE) {
                continue;
            }

            List<ContributionFondsAide> impayees = contributionRepository
                    .findAllByAideIdOrderByCreatedAtAsc(aide.getId()).stream()
                    .filter(c -> c.getStatut() == ContributionFondsAide.Statut.A_PAYER)
                    .toList();

            // Marquée traitée dans tous les cas : soit elle est soldée, soit elle
            // vient d'être signalée. On ne relance qu'une fois.
            aide.setRelanceRecouvrementEnvoyee(true);
            aideRepository.save(aide);

            if (impayees.isEmpty()) {
                continue; // recouvrement termine dans les temps
            }

            String libelle = aide.getRubrique() != null ? aide.getRubrique().getLibelle() : "Aide";
            String beneficiaire = aide.getMembre().getNom() + " " + aide.getMembre().getPrenom();

            for (ContributionFondsAide c : impayees) {
                if (c.getMembre().getUser() == null) {
                    continue;
                }
                notificationService.notifier(c.getMembre().getUser(),
                        Notification.Type.AIDE_RECOUVREMENT_RETARD,
                        "Part d'aide en retard",
                        "Votre part de %s FCFA pour l'aide « %s » (%s) devait être versée avant le %s."
                                .formatted(c.getMontant(), libelle, beneficiaire,
                                        aide.getDateEcheanceRecouvrement()),
                        aide.getId(), "AIDE");
            }

            notifierBureau(aide, libelle, beneficiaire, impayees.size());
            alertees++;
            log.info("[Scheduler] Aide {} en retard de recouvrement — {} part(s) impayée(s)",
                    aide.getId(), impayees.size());
        }

        log.info("[Scheduler] Recouvrement aides — {} échue(s) examinée(s), {} signalée(s)",
                echues.size(), alertees);
    }

    /** Alerte le bureau de la tontine concernée — pas les admins des autres tontines. */
    private void notifierBureau(Aide aide, String libelle, String beneficiaire, int nbImpayees) {
        membreRepository.findAllByTontineIdAndStatut(
                        aide.getMembre().getTontine().getId(), Membre.Statut.ACTIF).stream()
                .filter(m -> m.getFonction() != null && BUREAU.contains(m.getFonction()))
                .filter(m -> m.getUser() != null)
                .forEach(m -> notificationService.notifier(m.getUser(),
                        Notification.Type.AIDE_RECOUVREMENT_RETARD,
                        "Aide non recouvrée dans le délai",
                        "L'aide « %s » de %s a dépassé son délai de recouvrement (%s). %d part(s) restent à collecter."
                                .formatted(libelle, beneficiaire,
                                        aide.getDateEcheanceRecouvrement(), nbImpayees),
                        aide.getId(), "AIDE"));
    }
}
