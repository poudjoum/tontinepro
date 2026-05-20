package com.tontinepro.tontinepro_backend.api.cotisation;

import com.tontinepro.tontinepro_backend.api.cotisation.dto.CotisationResponse;
import com.tontinepro.tontinepro_backend.api.cotisation.dto.CreateCotisationRequest;
import com.tontinepro.tontinepro_backend.api.cotisation.dto.EnregistrerPaiementRequest;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CotisationService {

    private final CotisationRepository cotisationRepository;
    private final MembreRepository membreRepository;
    private final NotificationService notificationService;
    private final SanctionRepository sanctionRepository;

    @Transactional
    public CotisationResponse create(CreateCotisationRequest request) {
        Membre membre = membreRepository.findById(request.membreId())
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + request.membreId()));

        if (membre.getStatut() != Membre.Statut.ACTIF) {
            throw new IllegalArgumentException("Impossible d'enregistrer une cotisation pour un membre non actif");
        }

        if (cotisationRepository.existsByMembreIdAndMoisAndAnnee(
                membre.getId(), request.mois(), request.annee())) {
            throw new IllegalArgumentException(
                    "Une cotisation existe déjà pour ce membre sur %02d/%d".formatted(request.mois(), request.annee()));
        }

        var montant = request.montant() != null
                ? request.montant()
                : membre.getTontine().getMontantCotisationMin();

        Cotisation cotisation = Cotisation.builder()
                .membre(membre)
                .tontine(membre.getTontine())
                .mois(request.mois())
                .annee(request.annee())
                .montant(montant)
                .build();

        return CotisationResponse.from(cotisationRepository.save(cotisation));
    }

    @Transactional(readOnly = true)
    public CotisationResponse getById(UUID id) {
        return cotisationRepository.findById(id)
                .map(CotisationResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Cotisation introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<CotisationResponse> list(UUID membreId, UUID tontineId, Short mois, Short annee, Cotisation.Statut statut) {
        List<Cotisation> cotisations;

        if (tontineId != null && mois != null && annee != null) {
            cotisations = cotisationRepository.findAllByTontineIdAndMoisAndAnnee(tontineId, mois, annee);
        } else if (membreId != null) {
            cotisations = cotisationRepository.findAllByMembreId(membreId);
        } else if (tontineId != null) {
            cotisations = cotisationRepository.findAllByTontineId(tontineId);
        } else if (statut != null) {
            cotisations = cotisationRepository.findAllByStatut(statut);
        } else {
            cotisations = cotisationRepository.findAll();
        }

        return cotisations.stream().map(CotisationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CotisationResponse> getMe(String email) {
        return membreRepository.findByUserEmail(email)
                .map(m -> cotisationRepository.findAllByMembreId(m.getId())
                        .stream().map(CotisationResponse::from).toList())
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));
    }

    @Transactional
    public CotisationResponse enregistrerPaiement(UUID id, EnregistrerPaiementRequest request) {
        Cotisation cotisation = cotisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cotisation introuvable : " + id));

        if (cotisation.getStatut() == Cotisation.Statut.PAYEE) {
            throw new IllegalArgumentException("Cette cotisation est déjà marquée comme payée");
        }

        cotisation.setStatut(Cotisation.Statut.PAYEE);
        cotisation.setDatePaiement(
                request.datePaiement() != null ? request.datePaiement() : OffsetDateTime.now());
        cotisation.setReferencePaiement(request.referencePaiement());

        // Montant tontine (part du pot) — si fourni, remplace la valeur par défaut
        if (request.montantTontine() != null) {
            cotisation.setMontant(request.montantTontine());
        }
        // Montant fonds d'aide de ce mois
        if (request.montantFondAide() != null) {
            cotisation.setMontantFondAide(request.montantFondAide());
        }

        CotisationResponse response = CotisationResponse.from(cotisationRepository.save(cotisation));

        notificationService.notifier(cotisation.getMembre().getUser(),
                Notification.Type.COTISATION_PAYEE,
                "Cotisation enregistrée",
                "Votre cotisation de %02d/%d (%s FCFA) a bien été enregistrée."
                        .formatted(cotisation.getMois(), cotisation.getAnnee(), cotisation.getMontant()),
                cotisation.getId(), "COTISATION");

        return response;
    }

    @Transactional
    public CotisationResponse marquerEnRetard(UUID id) {
        Cotisation cotisation = cotisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cotisation introuvable : " + id));

        if (cotisation.getStatut() == Cotisation.Statut.PAYEE) {
            throw new IllegalArgumentException("Impossible de marquer en retard une cotisation déjà payée");
        }

        cotisation.setStatut(Cotisation.Statut.EN_RETARD);
        CotisationResponse response = CotisationResponse.from(cotisationRepository.save(cotisation));

        // Générer automatiquement une sanction RETARD_COTISATION (idempotent — une seule par cotisation)
        BigDecimal penalite = cotisation.getTontine().getMontantPenaliteRetard();
        if (penalite != null && penalite.compareTo(BigDecimal.ZERO) > 0
                && !sanctionRepository.existsByReferenceIdAndTypeSanction(
                        cotisation.getId(), Sanction.TypeSanction.RETARD_COTISATION)) {

            Sanction sanction = sanctionRepository.save(Sanction.builder()
                    .membre(cotisation.getMembre())
                    .tontine(cotisation.getTontine())
                    .typeSanction(Sanction.TypeSanction.RETARD_COTISATION)
                    .montant(penalite)
                    .motif("Retard de cotisation %02d/%d".formatted(cotisation.getMois(), cotisation.getAnnee()))
                    .referenceId(cotisation.getId())
                    .build());

            notificationService.notifier(cotisation.getMembre().getUser(),
                    Notification.Type.SANCTION_INFLIGEE,
                    "Sanction pour retard de cotisation",
                    "Une sanction de %s FCFA a été enregistrée pour retard de cotisation %02d/%d."
                            .formatted(penalite, cotisation.getMois(), cotisation.getAnnee()),
                    sanction.getId(), "SANCTION");
        }

        notificationService.notifier(cotisation.getMembre().getUser(),
                Notification.Type.COTISATION_EN_RETARD,
                "Cotisation en retard",
                "Votre cotisation de %02d/%d est en retard. Veuillez régulariser votre situation."
                        .formatted(cotisation.getMois(), cotisation.getAnnee()),
                cotisation.getId(), "COTISATION");

        return response;
    }
}
