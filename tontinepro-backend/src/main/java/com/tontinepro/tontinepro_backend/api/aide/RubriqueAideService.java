package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.RubriqueAideRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.RubriqueAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.SimulationAideResponse;
import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAide;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAideRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RubriqueAideService {

    private final RubriqueAideRepository rubriqueRepository;
    private final TontineRepository tontineRepository;
    private final MembreRepository membreRepository;

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
                .limiteParBeneficiaire(request.limiteParBeneficiaire())
                .porteeLimite(request.porteeLimite() != null
                        ? request.porteeLimite() : RubriqueAide.PorteeLimite.VIE)
                .variantes(normaliserVariantes(request.variantes()))
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
        rubrique.setLimiteParBeneficiaire(request.limiteParBeneficiaire());
        if (request.porteeLimite() != null)  rubrique.setPorteeLimite(request.porteeLimite());
        rubrique.setVariantes(normaliserVariantes(request.variantes()));

        return RubriqueAideResponse.from(rubriqueRepository.save(rubrique));
    }

    @Transactional
    public void supprimer(UUID id) {
        if (!rubriqueRepository.existsById(id)) {
            throw new IllegalArgumentException("Rubrique d'aide introuvable : " + id);
        }
        rubriqueRepository.deleteById(id);
    }

    /** Calcule part par membre et total pour une rubrique, avec le N courant de sa tontine. */
    @Transactional(readOnly = true)
    public SimulationAideResponse simuler(UUID rubriqueId) {
        RubriqueAide rubrique = rubriqueRepository.findById(rubriqueId)
                .orElseThrow(() -> new IllegalArgumentException("Rubrique d'aide introuvable : " + rubriqueId));
        int n = membreRepository.findAllByTontineIdAndStatut(
                rubrique.getTontine().getId(), Membre.Statut.ACTIF).size();
        return calculer(rubrique, n);
    }

    /**
     * Pas d'arrondi des parts, en FCFA. Une part doit pouvoir se compter avec les
     * coupures réellement en circulation : on ne demande pas 4 166,67 ni même
     * 4 166 FCFA à un membre, mais 4 150. Changer cette valeur suffit à changer
     * la granularité de toutes les parts.
     */
    private static final BigDecimal PAS_ARRONDI_PART = BigDecimal.valueOf(50);

    /**
     * Calcul pur (réutilisé à l'activation) : N = membres actifs (bénéficiaire inclus).
     *   PAR_PERSONNE : part = montantReference ; total = part × N
     *   FORFAITAIRE  : total = montantReference ; part = total ÷ N arrondi au pas supérieur
     *
     * <p>L'arrondi va vers le haut, et <strong>tous les membres paient la même
     * part</strong>, bénéficiaire compris. La collecte dépasse donc légèrement le
     * montant de l'aide — 12 × 4 200 = 50 400 pour une aide de 50 000 — et
     * l'excédent reste acquis au fonds. C'est un choix de la tontine : l'égalité
     * des parts prime, et le surplus constitue un coussin pour un fonds que
     * chaque préfinancement fait passer en négatif.</p>
     *
     * <p>Ne pas revenir à un arrondi vers le bas avec reliquat sur le bénéficiaire :
     * cela le faisait payer plus que les autres, ce que la tontine a écarté.</p>
     *
     * <p>Le mode PAR_PERSONNE n'est pas arrondi : son montant est saisi tel quel
     * par le bureau, ce n'est pas le résultat d'une division.</p>
     */
    public static SimulationAideResponse calculer(RubriqueAide r, int n) {
        BigDecimal ref = r.getMontantReference();
        BigDecimal part, total;
        if (r.getModeCalcul() == RubriqueAide.ModeCalcul.PAR_PERSONNE) {
            part = ref;
            total = ref.multiply(BigDecimal.valueOf(n));
        } else {
            total = ref;
            part = n > 0
                    ? ref.divide(BigDecimal.valueOf(n).multiply(PAS_ARRONDI_PART), 0, RoundingMode.UP)
                         .multiply(PAS_ARRONDI_PART)
                    : BigDecimal.ZERO;
        }
        return new SimulationAideResponse(
                r.getId(), r.getLibelle(), r.getTypeAide(), r.getModeCalcul(),
                ref, n, part, total, r.isPrefinancable());
    }

    /** Nettoie une liste de variantes saisie (« Père, Mère » → « Père,Mère »), null si vide. */
    static String normaliserVariantes(String raw) {
        List<String> items = parseVariantes(raw);
        return items.isEmpty() ? null : String.join(",", items);
    }

    /** Découpe une chaîne de variantes en liste propre (vide si null/blanc). */
    public static List<String> parseVariantes(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
