package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.ContributionFondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.FondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.MouvementFondsAideResponse;
import com.tontinepro.tontinepro_backend.domain.aide.*;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FondsAideService {

    private final FondsAideRepository fondsAideRepository;
    private final MouvementFondsAideRepository mouvementRepository;
    private final ContributionFondsAideRepository contributionRepository;
    private final MembreRepository membreRepository;

    @Transactional(readOnly = true)
    public FondsAideResponse getByTontineId(UUID tontineId) {
        return fondsAideRepository.findByTontineId(tontineId)
                .map(FondsAideResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Fonds d'aide introuvable pour la tontine : " + tontineId));
    }

    @Transactional(readOnly = true)
    public List<MouvementFondsAideResponse> getMouvements(UUID tontineId) {
        FondsAide fonds = loadFonds(tontineId);
        return mouvementRepository.findAllByFondsAideIdOrderByCreatedAtDesc(fonds.getId())
                .stream().map(MouvementFondsAideResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ContributionFondsAideResponse> getContributions(UUID tontineId, ContributionFondsAide.Statut statut) {
        FondsAide fonds = loadFonds(tontineId);
        List<ContributionFondsAide> contributions = statut != null
                ? contributionRepository.findAllByFondsAideIdAndStatut(fonds.getId(), statut)
                : contributionRepository.findAllByFondsAideId(fonds.getId());
        return contributions.stream().map(ContributionFondsAideResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ContributionFondsAideResponse> getMesContributions(String email) {
        return getMesContributions(email, null);
    }

    @Transactional(readOnly = true)
    public List<ContributionFondsAideResponse> getMesContributions(String email, java.util.UUID tontineId) {
        var membre = tontineId != null
                ? membreRepository.findByUserEmailAndTontineId(email, tontineId)
                : membreRepository.findByUserEmail(email);
        return membre
                .map(m -> contributionRepository.findAllByMembreId(m.getId())
                        .stream().map(ContributionFondsAideResponse::from).toList())
                .orElse(List.of());
    }

    /**
     * Mode MENSUEL — génère les enregistrements de contribution pour tous les membres actifs
     * d'une tontine pour la période donnée.
     */
    @Transactional
    public List<ContributionFondsAideResponse> genererContributionsMensuelles(UUID tontineId, short mois, short annee) {
        FondsAide fonds = loadFonds(tontineId);
        Tontine tontine = fonds.getTontine();

        if (tontine.getModeContributionAide() != Tontine.ModeContributionAide.MENSUEL) {
            throw new IllegalArgumentException("Cette tontine n'est pas configurée en mode MENSUEL");
        }
        if (tontine.getMontantCotisationAide() == null) {
            throw new IllegalStateException("montantCotisationAide non configuré sur la tontine");
        }

        List<Membre> membresActifs = membreRepository.findAllByTontineIdAndStatut(tontineId, Membre.Statut.ACTIF);

        List<ContributionFondsAide> nouvelles = membresActifs.stream()
                .filter(m -> !contributionRepository.existsByFondsAideIdAndMembreIdAndMoisAndAnnee(
                        fonds.getId(), m.getId(), mois, annee))
                .map(m -> ContributionFondsAide.builder()
                        .fondsAide(fonds)
                        .membre(m)
                        .montant(tontine.getMontantCotisationAide())
                        .mois(mois)
                        .annee(annee)
                        .build())
                .toList();

        return contributionRepository.saveAll(nouvelles)
                .stream().map(ContributionFondsAideResponse::from).toList();
    }

    /**
     * Enregistre le paiement d'une contribution au fonds d'aide et crédite le solde.
     */
    @Transactional
    public ContributionFondsAideResponse enregistrerPaiement(UUID contributionId) {
        ContributionFondsAide contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new IllegalArgumentException("Contribution introuvable : " + contributionId));

        if (contribution.getStatut() == ContributionFondsAide.Statut.PAYEE) {
            throw new IllegalArgumentException("Cette contribution est déjà payée");
        }

        FondsAide fonds = contribution.getFondsAide();
        fonds.setSolde(fonds.getSolde().add(contribution.getMontant()));
        fondsAideRepository.save(fonds);

        mouvementRepository.save(MouvementFondsAide.builder()
                .fondsAide(fonds)
                .typeMouvement(MouvementFondsAide.TypeMouvement.CONTRIBUTION)
                .montant(contribution.getMontant())
                .soldeApres(fonds.getSolde())
                .membre(contribution.getMembre())
                .description("Contribution fonds d'aide — membre " + contribution.getMembre().getMatricule())
                .build());

        contribution.setStatut(ContributionFondsAide.Statut.PAYEE);
        contribution.setDatePaiement(OffsetDateTime.now());
        return ContributionFondsAideResponse.from(contributionRepository.save(contribution));
    }

    FondsAide loadFonds(UUID tontineId) {
        return fondsAideRepository.findByTontineId(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Fonds d'aide introuvable pour la tontine : " + tontineId));
    }
}
