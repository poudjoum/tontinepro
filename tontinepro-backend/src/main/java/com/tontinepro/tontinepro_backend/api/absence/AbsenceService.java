package com.tontinepro.tontinepro_backend.api.absence;

import com.tontinepro.tontinepro_backend.api.absence.dto.AbsenceResponse;
import com.tontinepro.tontinepro_backend.api.absence.dto.EnregistrerAbsenceRequest;
import com.tontinepro.tontinepro_backend.domain.absence.Absence;
import com.tontinepro.tontinepro_backend.domain.absence.AbsenceRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final SanctionRepository sanctionRepository;

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
            Sanction sanction = Sanction.builder()
                    .membre(membre)
                    .tontine(membre.getTontine())
                    .typeSanction(Sanction.TypeSanction.ABSENCE_REUNION)
                    .montant(montantAmende)
                    .motif("Absence non justifiée à la réunion du " + request.dateReunion())
                    .referenceId(absence.getId())
                    .build();
            sanctionRepository.save(sanction);
        }

        return AbsenceResponse.from(absence);
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
