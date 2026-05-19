package com.tontinepro.tontinepro_backend.api.pret;

import com.tontinepro.tontinepro_backend.api.pret.dto.*;
import com.tontinepro.tontinepro_backend.domain.pret.Pret;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prets")
@RequiredArgsConstructor
@Tag(name = "PrÃªts")
public class PretController {

    private final PretService pretService;

    // â”€â”€ Endpoints membre â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping("/demande")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Soumettre une demande de prÃªt")
    public PretResponse soumettreDemande(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody DemandePretRequest request
    ) {
        return pretService.soumettreDemande(principal.getUsername(), request);
    }

    @GetMapping("/mes-prets")
    @Operation(summary = "Mes prÃªts")
    public List<PretResponse> getMesPrets(@AuthenticationPrincipal UserDetails principal) {
        return pretService.getMesPrets(principal.getUsername());
    }

    @GetMapping("/{id}/echeances")
    @Operation(summary = "Tableau d'amortissement d'un prÃªt")
    public List<EcheancePretResponse> getEcheances(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return pretService.getEcheances(id, isAdmin ? null : principal.getUsername());
    }

    @PostMapping("/{id}/rembourser")
    @Operation(summary = "Rembourser la prochaine Ã©chÃ©ance")
    public EcheancePretResponse rembourser(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return pretService.rembourserProchaineEcheance(id, principal.getUsername());
    }

    @GetMapping("/simulation")
    @Operation(summary = "Simuler un prÃªt (mensualitÃ©, tableau d'amortissement, coÃ»t total)")
    public SimulationPretResponse simuler(
            @RequestParam BigDecimal montant,
            @RequestParam short duree,
            @RequestParam UUID tontineId
    ) {
        return pretService.simuler(montant, duree, tontineId);
    }

    // â”€â”€ Endpoints admin â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Lister tous les prÃªts (filtrables par tontine ou statut)")
    public List<PretResponse> list(
            @RequestParam(required = false) UUID tontineId,
            @RequestParam(required = false) Pret.Statut statut
    ) {
        return pretService.list(tontineId, statut);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "DÃ©tails d'un prÃªt")
    public PretResponse getById(@PathVariable UUID id) {
        return pretService.getById(id);
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Valider et dÃ©caisser un prÃªt â€” gÃ©nÃ¨re l'Ã©chÃ©ancier")
    public PretResponse valider(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return pretService.valider(id, principal.getUsername());
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Rejeter une demande de prÃªt")
    public PretResponse rejeter(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody RejeterPretRequest request
    ) {
        return pretService.rejeter(id, principal.getUsername(), request);
    }

    @PatchMapping("/echeances/{echeanceId}/retard")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Marquer une Ã©chÃ©ance en retard")
    public EcheancePretResponse marquerEnRetard(@PathVariable UUID echeanceId) {
        return pretService.marquerEcheanceEnRetard(echeanceId);
    }
}

