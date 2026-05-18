package com.tontinepro.tontinepro_backend.api.cotisation;

import com.tontinepro.tontinepro_backend.api.cotisation.dto.CotisationResponse;
import com.tontinepro.tontinepro_backend.api.cotisation.dto.CreateCotisationRequest;
import com.tontinepro.tontinepro_backend.api.cotisation.dto.EnregistrerPaiementRequest;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CotisationService {

    private final CotisationRepository cotisationRepository;
    private final MembreRepository membreRepository;

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
                : membre.getTontine().getMontantCotisation();

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

        return CotisationResponse.from(cotisationRepository.save(cotisation));
    }

    @Transactional
    public CotisationResponse marquerEnRetard(UUID id) {
        Cotisation cotisation = cotisationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cotisation introuvable : " + id));

        if (cotisation.getStatut() == Cotisation.Statut.PAYEE) {
            throw new IllegalArgumentException("Impossible de marquer en retard une cotisation déjà payée");
        }

        cotisation.setStatut(Cotisation.Statut.EN_RETARD);
        return CotisationResponse.from(cotisationRepository.save(cotisation));
    }
}
