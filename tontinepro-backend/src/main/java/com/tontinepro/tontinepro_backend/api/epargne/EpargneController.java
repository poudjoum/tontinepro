package com.tontinepro.tontinepro_backend.api.epargne;

import com.tontinepro.tontinepro_backend.api.epargne.dto.CompteEpargneResponse;
import com.tontinepro.tontinepro_backend.api.epargne.dto.DepotRequest;
import com.tontinepro.tontinepro_backend.api.epargne.dto.MouvementEpargneResponse;
import com.tontinepro.tontinepro_backend.api.epargne.dto.RetraitRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/epargne")
@RequiredArgsConstructor
@Tag(name = "Épargne")
public class EpargneController {

    private final EpargneService epargneService;

    // ── Endpoints membre ────────────────────────────────────────────────

    @GetMapping("/mon-compte")
    @Operation(summary = "Mon compte épargne (solde)")
    public CompteEpargneResponse getMonCompte(@AuthenticationPrincipal UserDetails principal) {
        return epargneService.getMonCompte(principal.getUsername());
    }

    @PostMapping("/depot")
    @Operation(summary = "Effectuer un dépôt sur mon compte épargne")
    public CompteEpargneResponse depot(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody DepotRequest request
    ) {
        return epargneService.depot(principal.getUsername(), request);
    }

    @PostMapping("/retrait")
    @Operation(summary = "Effectuer un retrait de mon compte épargne")
    public CompteEpargneResponse retrait(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody RetraitRequest request
    ) {
        return epargneService.retrait(principal.getUsername(), request);
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique de mes transactions épargne")
    public List<MouvementEpargneResponse> getHistorique(@AuthenticationPrincipal UserDetails principal) {
        return epargneService.getHistorique(principal.getUsername());
    }

    // ── Endpoints admin ──────────────────────────────────────────────────

    @GetMapping("/comptes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister tous les comptes épargne (filtrable par tontine)")
    public List<CompteEpargneResponse> getAllComptes(
            @RequestParam(required = false) UUID tontineId
    ) {
        return epargneService.getAllComptes(tontineId);
    }

    @GetMapping("/comptes/{membreId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Compte épargne d'un membre")
    public CompteEpargneResponse getCompteByMembre(@PathVariable UUID membreId) {
        return epargneService.getCompteByMembre(membreId);
    }

    @GetMapping("/comptes/{membreId}/historique")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Historique épargne d'un membre")
    public List<MouvementEpargneResponse> getHistoriqueByMembre(@PathVariable UUID membreId) {
        return epargneService.getHistoriqueByMembre(membreId);
    }

    @PostMapping("/distribuer-interets")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Distribuer les intérêts sur tous les comptes d'une tontine")
    public Map<String, Object> distribuerInterets(@RequestParam UUID tontineId) {
        int nb = epargneService.distribuerInterets(tontineId);
        return Map.of("comptesCredites", nb);
    }
}
