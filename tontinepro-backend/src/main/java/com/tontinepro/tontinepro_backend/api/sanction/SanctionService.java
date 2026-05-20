package com.tontinepro.tontinepro_backend.api.sanction;

import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.api.sanction.dto.CreerSanctionRequest;
import com.tontinepro.tontinepro_backend.api.sanction.dto.SanctionResponse;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SanctionService {

    private final SanctionRepository sanctionRepository;
    private final MembreRepository membreRepository;
    private final NotificationService notificationService;

    @Transactional
    public SanctionResponse creer(CreerSanctionRequest request) {
        Membre membre = membreRepository.findById(request.membreId())
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + request.membreId()));

        Sanction sanction = Sanction.builder()
                .membre(membre)
                .tontine(membre.getTontine())
                .typeSanction(request.typeSanction())
                .montant(request.montant())
                .motif(request.motif())
                .build();

        SanctionResponse response = SanctionResponse.from(sanctionRepository.save(sanction));

        notificationService.notifier(membre.getUser(),
                Notification.Type.SANCTION_INFLIGEE,
                "Sanction enregistrée",
                "Une sanction de %s FCFA a été enregistrée à votre encontre. Motif : %s"
                        .formatted(request.montant(), request.motif() != null ? request.motif() : "Non précisé"),
                response.id(), "SANCTION");

        return response;
    }

    @Transactional
    public SanctionResponse marquerPayee(UUID id) {
        Sanction sanction = sanctionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sanction introuvable : " + id));

        if (sanction.isPayee()) {
            throw new IllegalArgumentException("Cette sanction est déjà marquée comme payée");
        }

        sanction.setPayee(true);
        return SanctionResponse.from(sanctionRepository.save(sanction));
    }

    @Transactional(readOnly = true)
    public List<SanctionResponse> listerParTontine(UUID tontineId, Boolean payee) {
        List<Sanction> sanctions;
        if (payee != null) {
            sanctions = sanctionRepository.findAllByTontineIdAndPayee(tontineId, payee);
        } else {
            sanctions = sanctionRepository.findAllByTontineId(tontineId);
        }
        return sanctions.stream().map(SanctionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SanctionResponse> mesSanctions(String email) {
        return membreRepository.findByUserEmail(email)
                .map(m -> sanctionRepository.findAllByMembreId(m.getId())
                        .stream().map(SanctionResponse::from).toList())
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));
    }
}
