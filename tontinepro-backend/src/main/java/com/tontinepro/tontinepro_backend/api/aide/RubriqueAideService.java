package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.RubriqueAideRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.RubriqueAideResponse;
import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAide;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAideRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RubriqueAideService {

    private final RubriqueAideRepository rubriqueRepository;
    private final TontineRepository tontineRepository;

    @Transactional(readOnly = true)
    public List<RubriqueAideResponse> lister(UUID tontineId, boolean actifSeulement) {
        List<RubriqueAide> rubriques = actifSeulement
                ? rubriqueRepository.findAllByTontineIdAndActifOrderByLibelleAsc(tontineId, true)
                : rubriqueRepository.findAllByTontineIdOrderByLibelleAsc(tontineId);
        return rubriques.stream().map(RubriqueAideResponse::from).toList();
    }

    @Transactional
    public RubriqueAideResponse creer(UUID tontineId, RubriqueAideRequest request) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + tontineId));

        RubriqueAide rubrique = RubriqueAide.builder()
                .tontine(tontine)
                .libelle(request.libelle().trim())
                .typeAide(request.typeAide() != null ? request.typeAide() : Aide.TypeAide.AUTRE)
                .modeCalcul(request.modeCalcul())
                .montantReference(request.montantReference())
                .prefinancable(request.prefinancable() == null || request.prefinancable())
                .actif(request.actif() == null || request.actif())
                .description(request.description())
                .build();

        return RubriqueAideResponse.from(rubriqueRepository.save(rubrique));
    }

    @Transactional
    public RubriqueAideResponse modifier(UUID id, RubriqueAideRequest request) {
        RubriqueAide rubrique = rubriqueRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rubrique d'aide introuvable : " + id));

        rubrique.setLibelle(request.libelle().trim());
        if (request.typeAide() != null)      rubrique.setTypeAide(request.typeAide());
        rubrique.setModeCalcul(request.modeCalcul());
        rubrique.setMontantReference(request.montantReference());
        if (request.prefinancable() != null) rubrique.setPrefinancable(request.prefinancable());
        if (request.actif() != null)         rubrique.setActif(request.actif());
        rubrique.setDescription(request.description());

        return RubriqueAideResponse.from(rubriqueRepository.save(rubrique));
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!rubriqueRepository.existsById(id)) {
            throw new IllegalArgumentException("Rubrique d'aide introuvable : " + id);
        }
        rubriqueRepository.deleteById(id);
    }
}
