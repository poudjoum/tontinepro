package com.tontinepro.tontinepro_backend.api.absence;

import com.tontinepro.tontinepro_backend.api.absence.dto.AbsenceResponse;
import com.tontinepro.tontinepro_backend.api.absence.dto.AppelPresenceRequest;
import com.tontinepro.tontinepro_backend.api.absence.dto.AppelPresenceResult;
import com.tontinepro.tontinepro_backend.api.absence.dto.EnregistrerAbsenceRequest;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.absence.Absence;
import com.tontinepro.tontinepro_backend.domain.absence.AbsenceRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final SanctionRepository sanctionRepository;
    private final TontineRepository tontineRepository;
    private final NotificationService notificationService;

    @Transactional
    public AbsenceResponse enregistrer(EnregistrerAbsenceRequest request, String enregistrePar) {
        Membre membre = membreRepository.findById(request.membreId())
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + request.membreId()));

        if (absenceRepository.existsByMembreIdAndDateReunion(request.membreId(), request.dateReunion())) {
            throw new IllegalArgumentException("Une absence a déjà été enregistrée pour ce membre à cette date");
        }

        User auteur = userRepository.findByEmail(enregistrePar)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        Absence absence = Absence.builder()
                .membre(membre)
                .tontine(membre.getTontine())
                .dateReunion(request.dateReunion())
                .justifiee(request.justifiee())
                .motif(request.motif())
                .enregistreePar(auteur)
                .build();

        absence = absenceRepository.save(absence);

        // Générer automatiquement une sanction si l'absence n'est pas justifiée
        BigDecimal montantAmende = membre.getTontine().getMontantAmende();
        if (!request.justifiee() && montantAmende != null && montantAmende.compareTo(BigDecimal.ZERO) > 0) {
            Sanction sanction = sanctionRepository.save(Sanction.builder()
                    .membre(membre)
                    .tontine(membre.getTontine())
                    .typeSanction(Sanction.TypeSanction.ABSENCE_REUNION)
                    .montant(montantAmende)
                    .motif("Absence non justifiée à la réunion du " + request.dateReunion())
                    .referenceId(absence.getId())
                    .build());

            notificationService.notifier(membre.getUser(),
                    Notification.Type.SANCTION_INFLIGEE,
                    "Sanction pour absence — réunion du " + request.dateReunion(),
                    "Une sanction de %s FCFA a été enregistrée pour absence non justifiée à la réunion du %s."
                            .formatted(montantAmende, request.dateReunion()),
                    sanction.getId(), "SANCTION");
        }

        return AbsenceResponse.from(absence);
    }

    /**
     * Appel de présence groupé pour une réunion.
     * Les membres actifs NON présents dans la liste sont automatiquement marqués absents.
     */
    @Transactional
    public AppelPresenceResult appelPresence(AppelPresenceRequest request, String enregistrePar) {
        Tontine tontine = tontineRepository.findById(request.tontineId())
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable"));

        User auteur = userRepository.findByEmail(enregistrePar)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        List<Membre> membresActifs = membreRepository.findAllByTontineIdAndStatut(
                tontine.getId(), Membre.Statut.ACTIF);

        BigDecimal montantAmende = tontine.getMontantAmende();

        int presentsCount = 0;
        int absentsCount = 0;
        int sanctionsCount = 0;
        List<AbsenceResponse> absencesCreees = new ArrayList<>();

        for (Membre membre : membresActifs) {
            boolean estPresent = request.membresPresentsIds().contains(membre.getId());
            if (estPresent) {
                presentsCount++;
                continue;
            }

            // Déjà enregistré pour cette réunion → skip
            if (absenceRepository.existsByMembreIdAndDateReunion(membre.getId(), request.dateReunion())) {
                absentsCount++;
                continue;
            }

            Absence absence = absenceRepository.save(Absence.builder()
                    .membre(membre)
                    .tontine(tontine)
                    .dateReunion(request.dateReunion())
                    .justifiee(false)
                    .motif("Absence enregistrée via appel de présence")
                    .enregistreePar(auteur)
                    .build());

            absencesCreees.add(AbsenceResponse.from(absence));
            absentsCount++;

            if (montantAmende != null && montantAmende.compareTo(BigDecimal.ZERO) > 0) {
                Sanction sanction = sanctionRepository.save(Sanction.builder()
                        .membre(membre)
                        .tontine(tontine)
                        .typeSanction(Sanction.TypeSanction.ABSENCE_REUNION)
                        .montant(montantAmende)
                        .motif("Absence non justifiée à la réunion du " + request.dateReunion())
                        .referenceId(absence.getId())
                        .build());

                notificationService.notifier(membre.getUser(),
                        Notification.Type.SANCTION_INFLIGEE,
                        "Sanction pour absence — réunion du " + request.dateReunion(),
                        "Une sanction de %s FCFA a été enregistrée pour absence à la réunion du %s."
                                .formatted(montantAmende, request.dateReunion()),
                        sanction.getId(), "SANCTION");
                sanctionsCount++;
            }
        }

        return new AppelPresenceResult(
                membresActifs.size(), presentsCount, absentsCount, sanctionsCount, absencesCreees);
    }

    @Transactional(readOnly = true)
    public List<AbsenceResponse> listerParTontine(UUID tontineId) {
        return absenceRepository.findAllByTontineId(tontineId)
                .stream().map(AbsenceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AbsenceResponse> mesSAbsences(String email) {
        Membre membre = membreRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));
        return absenceRepository.findAllByMembreId(membre.getId())
                .stream().map(AbsenceResponse::from).toList();
    }
}
