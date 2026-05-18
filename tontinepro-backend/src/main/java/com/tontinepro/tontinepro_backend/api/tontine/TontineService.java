package com.tontinepro.tontinepro_backend.api.tontine;

import com.tontinepro.tontinepro_backend.api.tontine.dto.CreateTontineRequest;
import com.tontinepro.tontinepro_backend.api.tontine.dto.TontineResponse;
import com.tontinepro.tontinepro_backend.api.tontine.dto.UpdateTontineConfigRequest;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TontineService {

    private final TontineRepository tontineRepository;

    @Transactional
    public TontineResponse create(CreateTontineRequest request) {
        if (tontineRepository.existsByNom(request.nom())) {
            throw new IllegalArgumentException("Une tontine avec le nom « " + request.nom() + " » existe déjà");
        }

        Tontine tontine = Tontine.builder()
                .nom(request.nom())
                .description(request.description())
                .montantCotisation(request.montantCotisation())
                .jourCotisation(request.jourCotisation())
                .tauxInteretPret(request.tauxInteretPret())
                .tauxInteretEpargne(request.tauxInteretEpargne())
                .build();

        return TontineResponse.from(tontineRepository.save(tontine));
    }

    @Transactional(readOnly = true)
    public TontineResponse getById(UUID id) {
        return tontineRepository.findById(id)
                .map(TontineResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<TontineResponse> listActive() {
        return tontineRepository.findAllByActifTrue()
                .stream()
                .map(TontineResponse::from)
                .toList();
    }

    @Transactional
    public TontineResponse updateConfig(UUID id, UpdateTontineConfigRequest request) {
        Tontine tontine = tontineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + id));

        if (request.nom() != null && !request.nom().equals(tontine.getNom())) {
            if (tontineRepository.existsByNom(request.nom())) {
                throw new IllegalArgumentException("Une tontine avec le nom « " + request.nom() + " » existe déjà");
            }
            tontine.setNom(request.nom());
        }
        if (request.description() != null) {
            tontine.setDescription(request.description());
        }
        if (request.montantCotisation() != null) {
            tontine.setMontantCotisation(request.montantCotisation());
        }
        if (request.jourCotisation() != null) {
            tontine.setJourCotisation(request.jourCotisation());
        }
        if (request.tauxInteretPret() != null) {
            tontine.setTauxInteretPret(request.tauxInteretPret());
        }
        if (request.tauxInteretEpargne() != null) {
            tontine.setTauxInteretEpargne(request.tauxInteretEpargne());
        }
        if (request.actif() != null) {
            tontine.setActif(request.actif());
        }

        return TontineResponse.from(tontineRepository.save(tontine));
    }
}
