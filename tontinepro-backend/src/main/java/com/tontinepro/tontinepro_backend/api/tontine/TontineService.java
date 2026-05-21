package com.tontinepro.tontinepro_backend.api.tontine;

import com.tontinepro.tontinepro_backend.api.membre.dto.MembreResponse;
import com.tontinepro.tontinepro_backend.api.tontine.dto.CreateTontineRequest;
import com.tontinepro.tontinepro_backend.api.tontine.dto.RejoindreOuvertRequest;
import com.tontinepro.tontinepro_backend.api.tontine.dto.TontineResponse;
import com.tontinepro.tontinepro_backend.api.tontine.dto.UpdateTontineConfigRequest;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TontineService {

    private final TontineRepository tontineRepository;
    private final FondsAideRepository fondsAideRepository;
    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final CompteEpargneRepository compteEpargneRepository;

    @Transactional
    public TontineResponse create(CreateTontineRequest request) {
        if (tontineRepository.existsByNom(request.nom())) {
            throw new IllegalArgumentException("Une tontine avec le nom « " + request.nom() + " » existe déjà");
        }

        var mode = request.modeContributionAide() != null
                ? request.modeContributionAide()
                : Tontine.ModeContributionAide.AUCUN;

        if (mode == Tontine.ModeContributionAide.MENSUEL && request.montantCotisationAide() == null) {
            throw new IllegalArgumentException("montantCotisationAide est obligatoire pour le mode MENSUEL");
        }

        Tontine tontine = Tontine.builder()
                .nom(request.nom())
                .description(request.description())
                .montantCotisationMin(request.montantCotisationMin())
                .montantCotisationMax(request.montantCotisationMax())
                .montantConsensuel(request.montantConsensuel())
                .jourReference(request.jourReference() != null ? request.jourReference() : 1)
                .typeReglePeriodicite(request.typeReglePeriodicite() != null
                        ? request.typeReglePeriodicite()
                        : Tontine.TypeReglePeriodicite.MENSUEL_JOUR_FIXE)
                .dateProchaineTontine(request.dateProchaineTontine())
                .tauxInteretPret(request.tauxInteretPret())
                .tauxInteretEpargne(request.tauxInteretEpargne())
                .modeContributionAide(mode)
                .montantCotisationAide(request.montantCotisationAide())
                .build();

        Tontine saved = tontineRepository.save(tontine);

        fondsAideRepository.save(FondsAide.builder().tontine(saved).build());

        return TontineResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TontineResponse getById(UUID id) {
        return tontineRepository.findById(id)
                .map(TontineResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<TontineResponse> listActive(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Le super-admin voit toutes les tontines
        if (user.getRole() == User.Role.SUPER_ADMIN) {
            return tontineRepository.findAllByActifTrue()
                    .stream().map(TontineResponse::from).toList();
        }

        // Les autres voient uniquement les tontines auxquelles ils appartiennent
        return membreRepository.findAllByUserEmail(email).stream()
                .map(m -> m.getTontine())
                .filter(Tontine::isActif)
                .map(TontineResponse::from)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TontineResponse> listPubliques() {
        return tontineRepository.findAllByActifTrueAndVisibleTrue()
                .stream().map(TontineResponse::from).toList();
    }

    /**
     * Rejoindre directement une tontine OUVERTE sans demande d'approbation.
     * Le membre est créé immédiatement avec statut ACTIF.
     */
    @Transactional
    public MembreResponse rejoindreOuverte(UUID tontineId, RejoindreOuvertRequest request, String email) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable"));

        if (!tontine.isActif()) {
            throw new IllegalArgumentException("Cette tontine n'est plus active");
        }
        if (tontine.getTypeAcces() == Tontine.TypeAcces.RESTREINTE) {
            throw new IllegalArgumentException(
                    "Cette tontine est restreinte à un groupe spécifique. Contactez l'administrateur.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (membreRepository.existsByUserIdAndTontineId(user.getId(), tontineId)) {
            throw new IllegalArgumentException("Vous êtes déjà membre de cette tontine");
        }

        String matricule = "MBR-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        Membre membre = membreRepository.save(Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(request.nom())
                .prenom(request.prenom())
                .matricule(matricule)
                .build());

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return MembreResponse.from(membre);
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
        if (request.montantCotisationMin() != null) {
            tontine.setMontantCotisationMin(request.montantCotisationMin());
        }
        if (request.montantCotisationMax() != null) {
            tontine.setMontantCotisationMax(request.montantCotisationMax());
        }
        if (request.montantConsensuel() != null) {
            tontine.setMontantConsensuel(request.montantConsensuel());
        }
        if (request.jourReference() != null) {
            tontine.setJourReference(request.jourReference());
        }
        if (request.typeReglePeriodicite() != null) {
            tontine.setTypeReglePeriodicite(request.typeReglePeriodicite());
        }
        if (request.dateProchaineTontine() != null) {
            tontine.setDateProchaineTontine(request.dateProchaineTontine());
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
        if (request.modeContributionAide() != null) {
            if (request.modeContributionAide() == Tontine.ModeContributionAide.MENSUEL
                    && request.montantCotisationAide() == null
                    && tontine.getMontantCotisationAide() == null) {
                throw new IllegalArgumentException("montantCotisationAide est obligatoire pour le mode MENSUEL");
            }
            tontine.setModeContributionAide(request.modeContributionAide());
        }
        if (request.montantCotisationAide() != null) {
            tontine.setMontantCotisationAide(request.montantCotisationAide());
        }
        if (request.montantAmende() != null) {
            tontine.setMontantAmende(request.montantAmende());
        }
        if (request.montantPenaliteRetard() != null) {
            tontine.setMontantPenaliteRetard(request.montantPenaliteRetard());
        }

        return TontineResponse.from(tontineRepository.save(tontine));
    }
}
