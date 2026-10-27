package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.ArriereRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.ArriereResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.ContributionFondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.ReglementArriereRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.EncaisserRattrapageRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.FondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.MouvementFondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.VersementsAnterieursRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.VersementsAnterieursResponse;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAide;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fonds-aide")
@RequiredArgsConstructor
@Tag(name = "Fonds d'Aide")
public class FondsAideController {

    private final FondsAideService fondsAideService;
    private final ArriereService arriereService;

    @GetMapping("/{tontineId}/arrieres")
    @PreAuthorize("@sec.bureauEncaisse(authentication.name, #tontineId)")
    @Operation(summary = "Arriérés des membres (fonds non cotisé avant l'application) et leurs règlements")
    public List<ArriereResponse> listerArrieres(@PathVariable UUID tontineId) {
        return arriereService.lister(tontineId);
    }

    @PostMapping("/{tontineId}/arrieres")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId)")
    @Operation(summary = "Enregistrer l'arriéré d'un membre")
    public List<ArriereResponse> creerArriere(@PathVariable UUID tontineId,
                                              @Valid @RequestBody ArriereRequest request) {
        return arriereService.creer(tontineId, request.membreId(), request.libelle(), request.montant(),
                request.dateConstat());
    }

    @DeleteMapping("/{tontineId}/arrieres/{arriereId}")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId)")
    @Operation(summary = "Supprimer un arriéré saisi par erreur (sans règlement)")
    public List<ArriereResponse> supprimerArriere(@PathVariable UUID tontineId, @PathVariable UUID arriereId) {
        return arriereService.supprimer(tontineId, arriereId);
    }

    @PostMapping("/{tontineId}/arrieres/{arriereId}/reglements")
    @PreAuthorize("@sec.bureauEncaisse(authentication.name, #tontineId)")
    @Operation(summary = "Encaisser un règlement d'arriéré (crédite le fonds d'aide)")
    public List<ArriereResponse> reglerArriere(@PathVariable UUID tontineId, @PathVariable UUID arriereId,
                                               @Valid @RequestBody ReglementArriereRequest request) {
        return arriereService.regler(tontineId, arriereId, request.montant(), request.dateReglement());
    }

    @DeleteMapping("/{tontineId}/arrieres/{arriereId}/reglements/{reglementId}")
    @PreAuthorize("@sec.bureauEncaisse(authentication.name, #tontineId)")
    @Operation(summary = "Annuler un règlement d'arriéré (débite le fonds d'aide)")
    public List<ArriereResponse> annulerReglementArriere(@PathVariable UUID tontineId, @PathVariable UUID arriereId,
                                                         @PathVariable UUID reglementId) {
        return arriereService.annulerReglement(tontineId, arriereId, reglementId);
    }

    @GetMapping("/{tontineId}")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId) "
            + "or @sec.peutEncaisser(authentication.name, #tontineId)")
    @Operation(summary = "Ã‰tat du fonds d'aide d'une tontine (solde, mode, montant par membre)")
    public FondsAideResponse getByTontineId(@PathVariable UUID tontineId) {
        return fondsAideService.getByTontineId(tontineId);
    }

    @GetMapping("/{tontineId}/mouvements")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId) "
            + "or @sec.peutEncaisser(authentication.name, #tontineId)")
    @Operation(summary = "Historique des mouvements du fonds d'aide")
    public List<MouvementFondsAideResponse> getMouvements(@PathVariable UUID tontineId) {
        return fondsAideService.getMouvements(tontineId);
    }

    @GetMapping("/{tontineId}/contributions")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId) "
            + "or @sec.peutEncaisser(authentication.name, #tontineId)")
    @Operation(summary = "Contributions des membres au fonds (filtrables par statut)")
    public List<ContributionFondsAideResponse> getContributions(
            @PathVariable UUID tontineId,
            @RequestParam(required = false) ContributionFondsAide.Statut statut
    ) {
        return fondsAideService.getContributions(tontineId, statut);
    }

    @PostMapping("/{tontineId}/contributions/generer")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId) "
            + "or @sec.peutEncaisser(authentication.name, #tontineId)")
    @Operation(summary = "GÃ©nÃ©rer les contributions mensuelles pour tous les membres actifs (mode MENSUEL)")
    public List<ContributionFondsAideResponse> genererContributionsMensuelles(
            @PathVariable UUID tontineId,
            @RequestParam short mois,
            @RequestParam short annee
    ) {
        return fondsAideService.genererContributionsMensuelles(tontineId, mois, annee);
    }

    @GetMapping("/{tontineId}/versements-anterieurs")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId) "
            + "or @sec.peutEncaisser(authentication.name, #tontineId)")
    @Operation(summary = "Fond de caisse versé avant l'application, par membre, pour une année")
    public VersementsAnterieursResponse getVersementsAnterieurs(
            @PathVariable UUID tontineId,
            @RequestParam short annee
    ) {
        return fondsAideService.getVersementsAnterieurs(tontineId, annee);
    }

    @PutMapping("/{tontineId}/versements-anterieurs")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId) "
            + "or @sec.peutEncaisser(authentication.name, #tontineId)")
    @Operation(summary = "Déclarer le fond de caisse versé avant l'application (crédite le fonds, "
            + "réduit la retenue au bénéfice)")
    public VersementsAnterieursResponse enregistrerVersementsAnterieurs(
            @PathVariable UUID tontineId,
            @Valid @RequestBody VersementsAnterieursRequest request
    ) {
        return fondsAideService.enregistrerVersementsAnterieurs(tontineId, request);
    }

    @PostMapping("/{tontineId}/rattrapage")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Encaisser le rattrapage de fin de session d'un membre (ses parts dues sur les "
            + "aides versées) — réservé au Trésorier de la tontine ; voir sec.peutEncaisser")
    public Map<String, BigDecimal> encaisserRattrapage(
            @PathVariable UUID tontineId,
            @Valid @RequestBody EncaisserRattrapageRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        BigDecimal total = fondsAideService.encaisserRattrapage(
                tontineId, request.membreId(), request.aideIds(), principal.getUsername());
        return Map.of("montantEncaisse", total);
    }

    @PatchMapping("/contributions/{id}/payer")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Enregistrer le paiement d'une contribution au fonds d'aide "
            + "(rÃ©servÃ© au TrÃ©sorier de la tontine ; voir sec.peutEncaisser)")
    public ContributionFondsAideResponse enregistrerPaiement(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return fondsAideService.enregistrerPaiement(id, principal.getUsername());
    }

    @GetMapping("/mes-contributions")
    @Operation(summary = "Mes contributions au fonds d'aide (tontine courante si tontineId fourni)")
    public List<ContributionFondsAideResponse> getMesContributions(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) UUID tontineId
    ) {
        return fondsAideService.getMesContributions(principal.getUsername(), tontineId);
    }
}

