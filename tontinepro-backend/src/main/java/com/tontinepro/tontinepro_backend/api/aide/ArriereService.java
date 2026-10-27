package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.ArriereResponse;
import com.tontinepro.tontinepro_backend.domain.aide.*;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Arriérés : fonds non cotisé par un membre avant l'arrivée de la tontine dans
 * l'application. Le bureau enregistre l'arriéré, puis ses règlements ; chaque règlement
 * crédite le fonds d'aide et est journalisé (ARRIERE), son annulation le débite
 * (ARRIERE_CORRECTION), de sorte que le journal reste réconcilié avec le solde.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArriereService {

    private final ArriereMembreRepository arriereRepository;
    private final ReglementArriereRepository reglementRepository;
    private final MembreRepository membreRepository;
    private final FondsAideRepository fondsAideRepository;
    private final MouvementFondsAideRepository mouvementRepository;

    @Transactional(readOnly = true)
    public List<ArriereResponse> lister(UUID tontineId) {
        Map<UUID, List<ReglementArriere>> reglements = reglementRepository.findAllByArriereTontineId(tontineId)
                .stream().collect(Collectors.groupingBy(r -> r.getArriere().getId()));
        return arriereRepository.findAllByTontineIdOrderByDateConstatAscCreatedAtAsc(tontineId).stream()
                .map(a -> ArriereResponse.from(a, reglements.getOrDefault(a.getId(), List.of()).stream()
                        .sorted(Comparator.comparing(ReglementArriere::getDateReglement)
                                .thenComparing(ReglementArriere::getCreatedAt))
                        .toList()))
                .toList();
    }

    @Transactional
    public List<ArriereResponse> creer(UUID tontineId, UUID membreId, String libelle, BigDecimal montant,
                                       LocalDate dateConstat) {
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + membreId));
        if (!membre.getTontine().getId().equals(tontineId)) {
            throw new IllegalArgumentException("Ce membre n'appartient pas à cette tontine");
        }
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le montant de l'arriéré doit être strictement positif");
        }
        LocalDate date = dateConstat != null ? dateConstat : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de l'arriéré ne peut pas être dans le futur");
        }
        arriereRepository.save(ArriereMembre.builder()
                .tontine(membre.getTontine())
                .membre(membre)
                .libelle(libelle == null || libelle.isBlank() ? "Fonds non cotisé" : libelle.trim())
                .montant(montant)
                .dateConstat(date)
                .build());
        log.info("[Arriéré] {} FCFA pour le membre {} (tontine {})", montant, membre.getMatricule(), tontineId);
        return lister(tontineId);
    }

    /** Supprime un arriéré saisi par erreur, tant qu'aucun règlement n'est enregistré. */
    @Transactional
    public List<ArriereResponse> supprimer(UUID tontineId, UUID arriereId) {
        ArriereMembre a = arriere(tontineId, arriereId);
        if (!reglementRepository.findAllByArriereIdOrderByDateReglementAscCreatedAtAsc(arriereId).isEmpty()) {
            throw new IllegalStateException("Cet arriéré a déjà des règlements : annulez-les d'abord");
        }
        arriereRepository.delete(a);
        return lister(tontineId);
    }

    /** Encaisse un règlement, sans dépasser le reste dû ; il crédite le fonds d'aide. */
    @Transactional
    public List<ArriereResponse> regler(UUID tontineId, UUID arriereId, BigDecimal montant, LocalDate date) {
        ArriereMembre a = arriere(tontineId, arriereId);
        if (montant == null || montant.signum() <= 0) {
            throw new IllegalArgumentException("Le règlement doit être strictement positif");
        }
        LocalDate jour = date != null ? date : LocalDate.now();
        if (jour.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date du règlement ne peut pas être dans le futur");
        }
        BigDecimal regle = reglementRepository.findAllByArriereIdOrderByDateReglementAscCreatedAtAsc(arriereId)
                .stream().map(ReglementArriere::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reste = a.getMontant().subtract(regle);
        if (montant.compareTo(reste) > 0) {
            throw new IllegalArgumentException("Le règlement dépasse le reste dû (" + reste.toPlainString() + " FCFA)");
        }
        reglementRepository.save(ReglementArriere.builder().arriere(a).montant(montant).dateReglement(jour).build());
        mouvementFonds(a, montant, MouvementFondsAide.TypeMouvement.ARRIERE,
                "Règlement d'arriéré — " + a.getLibelle());
        return lister(tontineId);
    }

    /** Annule un règlement saisi par erreur : le fonds d'aide est débité d'autant. */
    @Transactional
    public List<ArriereResponse> annulerReglement(UUID tontineId, UUID arriereId, UUID reglementId) {
        ArriereMembre a = arriere(tontineId, arriereId);
        ReglementArriere r = reglementRepository.findById(reglementId)
                .filter(x -> x.getArriere().getId().equals(arriereId))
                .orElseThrow(() -> new IllegalArgumentException("Règlement introuvable : " + reglementId));
        reglementRepository.delete(r);
        mouvementFonds(a, r.getMontant().negate(), MouvementFondsAide.TypeMouvement.ARRIERE_CORRECTION,
                "Règlement d'arriéré annulé — " + a.getLibelle());
        return lister(tontineId);
    }

    private void mouvementFonds(ArriereMembre a, BigDecimal delta, MouvementFondsAide.TypeMouvement type,
                                String description) {
        FondsAide fonds = fondsAideRepository.findByTontineId(a.getTontine().getId())
                .orElseThrow(() -> new IllegalArgumentException("Fonds d'aide introuvable pour cette tontine"));
        fonds.setSolde(fonds.getSolde().add(delta));
        fondsAideRepository.save(fonds);
        mouvementRepository.save(MouvementFondsAide.builder()
                .fondsAide(fonds)
                .typeMouvement(type)
                .montant(delta.abs())
                .soldeApres(fonds.getSolde())
                .membre(a.getMembre())
                .description(description + " — membre " + a.getMembre().getMatricule())
                .build());
    }

    private ArriereMembre arriere(UUID tontineId, UUID arriereId) {
        return arriereRepository.findById(arriereId)
                .filter(a -> a.getTontine().getId().equals(tontineId))
                .orElseThrow(() -> new IllegalArgumentException("Arriéré introuvable : " + arriereId));
    }
}
