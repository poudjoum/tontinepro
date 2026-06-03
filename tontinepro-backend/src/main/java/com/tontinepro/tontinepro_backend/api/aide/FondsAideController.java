package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.ContributionFondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.FondsAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.MouvementFondsAideResponse;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAide;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fonds-aide")
@RequiredArgsConstructor
@Tag(name = "Fonds d'Aide")
public class FondsAideController {

    private final FondsAideService fondsAideService;

    @GetMapping("/{tontineId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Ã‰tat du fonds d'aide d'une tontine (solde, mode, montant par membre)")
    public FondsAideResponse getByTontineId(@PathVariable UUID tontineId) {
        return fondsAideService.getByTontineId(tontineId);
    }

    @GetMapping("/{tontineId}/mouvements")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Historique des mouvements du fonds d'aide")
    public List<MouvementFondsAideResponse> getMouvements(@PathVariable UUID tontineId) {
        return fondsAideService.getMouvements(tontineId);
    }

    @GetMapping("/{tontineId}/contributions")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Contributions des membres au fonds (filtrables par statut)")
    public List<ContributionFondsAideResponse> getContributions(
            @PathVariable UUID tontineId,
            @RequestParam(required = false) ContributionFondsAide.Statut statut
    ) {
        return fondsAideService.getContributions(tontineId, statut);
    }

    @PostMapping("/{tontineId}/contributions/generer")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "GÃ©nÃ©rer les contributions mensuelles pour tous les membres actifs (mode MENSUEL)")
    public List<ContributionFondsAideResponse> genererContributionsMensuelles(
            @PathVariable UUID tontineId,
            @RequestParam short mois,
            @RequestParam short annee
    ) {
        return fondsAideService.genererContributionsMensuelles(tontineId, mois, annee);
    }

    @PatchMapping("/contributions/{id}/payer")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Enregistrer le paiement d'une contribution au fonds d'aide")
    public ContributionFondsAideResponse enregistrerPaiement(@PathVariable UUID id) {
        return fondsAideService.enregistrerPaiement(id);
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

