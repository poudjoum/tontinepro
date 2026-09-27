package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.ContributionFondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.FondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.MouvementFondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.VersementsAnterieursRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.VersementsAnterieursResponse;
import com.tontinepro.tontinepro_backend.domain.aide.*;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.infrastructure.security.SecurityExpressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FondsAideService {

    private final FondsAideRepository fondsAideRepository;
    private final MouvementFondsAideRepository mouvementRepository;
    private final ContributionFondsAideRepository contributionRepository;
    private final VersementAnterieurFondsRepository versementAnterieurRepository;
    private final MembreRepository membreRepository;
    private final SecurityExpressionService securityExpressionService;

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
    public ContributionFondsAideResponse enregistrerPaiement(UUID contributionId, String encaisseurEmail) {
        ContributionFondsAide contribution = contributionRepository.findById(contributionId)
                .orElseThrow(() -> new IllegalArgumentException("Contribution introuvable : " + contributionId));

        if (contribution.getStatut() == ContributionFondsAide.Statut.PAYEE) {
            throw new IllegalArgumentException("Cette contribution est déjà payée");
        }

        UUID tontineId = contribution.getMembre().getTontine().getId();
        if (!securityExpressionService.peutEncaisser(encaisseurEmail, tontineId)) {
            throw new AccessDeniedException(
                    "L'encaissement des contributions revient au Trésorier de la tontine");
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
                // Rattacher le mouvement à l'aide qu'il finance (null en mode
                // MENSUEL, où la contribution n'en dépend pas). Sans ce lien,
                // AideService.supprimer — qui retrouve les mouvements par
                // findAllByAideId — laissait derrière lui tous les crédits de
                // collecte : le solde était bien rembobiné, mais le journal des
                // mouvements gardait des lignes sans contrepartie et cessait de
                // se réconcilier avec le solde.
                .aide(contribution.getAide())
                .description("Contribution fonds d'aide — membre " + contribution.getMembre().getMatricule())
                .build());

        contribution.setStatut(ContributionFondsAide.Statut.PAYEE);
        contribution.setDatePaiement(OffsetDateTime.now());
        return ContributionFondsAideResponse.from(contributionRepository.save(contribution));
    }

    /**
     * Fond de caisse versé avant l'application, pour chaque membre actif de la
     * tontine sur l'année donnée (0 si rien n'a été déclaré).
     */
    @Transactional(readOnly = true)
    public VersementsAnterieursResponse getVersementsAnterieurs(UUID tontineId, short annee) {
        FondsAide fonds = loadFonds(tontineId);
        Map<UUID, BigDecimal> declares = versementAnterieurRepository
                .findAllByTontineIdAndAnnee(tontineId, annee).stream()
                .collect(Collectors.toMap(v -> v.getMembre().getId(),
                        VersementAnterieurFonds::getMontant, BigDecimal::add));

        List<VersementsAnterieursResponse.Ligne> lignes = membreRepository
                .findAllByTontineIdAndStatut(tontineId, Membre.Statut.ACTIF).stream()
                .map(m -> new VersementsAnterieursResponse.Ligne(
                        m.getId(), m.getMatricule(), m.getPrenom() + " " + m.getNom(),
                        declares.getOrDefault(m.getId(), BigDecimal.ZERO)))
                .sorted(Comparator.comparing(VersementsAnterieursResponse.Ligne::nomPrenom,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        BigDecimal total = lignes.stream().map(VersementsAnterieursResponse.Ligne::montant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal obligation = fonds.getTontine().getMontantFondAideAnnuelMembre();
        return new VersementsAnterieursResponse(annee,
                obligation != null ? obligation : BigDecimal.ZERO, lignes, total);
    }

    /**
     * Enregistre le fond de caisse versé avant l'application. Pour chaque membre,
     * l'écart avec la déclaration précédente est reporté sur le solde du fonds et
     * journalisé (REPRISE à la hausse, REPRISE_CORRECTION à la baisse), de sorte
     * que le journal des mouvements reste réconcilié avec le solde.
     */
    @Transactional
    public VersementsAnterieursResponse enregistrerVersementsAnterieurs(UUID tontineId,
                                                                        VersementsAnterieursRequest request) {
        FondsAide fonds = loadFonds(tontineId);
        short annee = request.annee();

        for (VersementsAnterieursRequest.Ligne ligne : request.lignes()) {
            Membre membre = membreRepository.findById(ligne.membreId())
                    .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + ligne.membreId()));
            if (!membre.getTontine().getId().equals(tontineId)) {
                throw new IllegalArgumentException("Le membre " + membre.getMatricule()
                        + " n'appartient pas à cette tontine");
            }

            BigDecimal nouveau = ligne.montant().setScale(2, RoundingMode.HALF_UP);
            var existant = versementAnterieurRepository.findByMembreIdAndAnnee(membre.getId(), annee);
            BigDecimal ancien = existant.map(VersementAnterieurFonds::getMontant).orElse(BigDecimal.ZERO);
            BigDecimal ecart = nouveau.subtract(ancien);
            if (ecart.signum() == 0) continue;

            if (nouveau.signum() == 0) {
                existant.ifPresent(versementAnterieurRepository::delete);
            } else {
                VersementAnterieurFonds v = existant.orElseGet(() -> VersementAnterieurFonds.builder()
                        .tontine(fonds.getTontine())
                        .membre(membre)
                        .annee(annee)
                        .build());
                v.setMontant(nouveau);
                versementAnterieurRepository.save(v);
            }

            fonds.setSolde(fonds.getSolde().add(ecart));
            mouvementRepository.save(MouvementFondsAide.builder()
                    .fondsAide(fonds)
                    .typeMouvement(ecart.signum() > 0
                            ? MouvementFondsAide.TypeMouvement.REPRISE
                            : MouvementFondsAide.TypeMouvement.REPRISE_CORRECTION)
                    .montant(ecart.abs())
                    .soldeApres(fonds.getSolde())
                    .membre(membre)
                    .description("Fond de caisse versé avant l'application (" + annee + ") — membre "
                            + membre.getMatricule())
                    .build());
        }
        fondsAideRepository.save(fonds);

        return getVersementsAnterieurs(tontineId, annee);
    }

    FondsAide loadFonds(UUID tontineId) {
        return fondsAideRepository.findByTontineId(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Fonds d'aide introuvable pour la tontine : " + tontineId));
    }
}
