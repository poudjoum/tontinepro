package com.tontinepro.tontinepro_backend.api.scheduler;

import com.tontinepro.tontinepro_backend.api.session.LotService;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontine;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontineRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Fige automatiquement les sessions « à lot » dont la période d'adhésion est échue
 * (≥ moisClotureAdhesions mois écoulés depuis le début de session).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FigeageLotScheduler {

    private final SessionTontineRepository sessionRepository;
    private final LotService lotService;

    /** Chaque nuit à 00:05 : fige les sessions à lot arrivées à échéance d'adhésion. */
    @Scheduled(cron = "0 5 0 * * *")
    public void figerSessionsEchues() {
        LocalDate today = LocalDate.now();
        List<SessionTontine> sessions =
                sessionRepository.findAllByStatut(SessionTontine.Statut.EN_COURS);

        for (SessionTontine session : sessions) {
            Tontine tontine = session.getTontine();
            if (tontine.getMode() != Tontine.ModeTontine.A_LOT || session.isFigee()) {
                continue;
            }
            LocalDate echeance = session.getDateDebut().plusMonths(tontine.getMoisClotureAdhesions());
            if (today.isBefore(echeance)) {
                continue; // période d'adhésion encore ouverte
            }
            try {
                lotService.figer(session.getId());
                log.info("[FigeageLot] Session {} figée automatiquement", session.getId());
            } catch (Exception e) {
                log.warn("[FigeageLot] Échec figeage session {} : {}", session.getId(), e.getMessage());
            }
        }
    }
}
