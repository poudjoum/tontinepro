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
@Tag(name = "Ã‰pargne")
public class EpargneController {

    private final EpargneService epargneService;

    // â”€â”€ Endpoints membre â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/mon-compte")
    @Operation(summary = "Mon compte Ã©pargne (solde) â€” tontine courante si tontineId fourni")
    public CompteEpargneResponse getMonCompte(@AuthenticationPrincipal UserDetails principal,
                                              @RequestParam(required = false) UUID tontineId) {
        return epargneService.getMonCompte(principal.getUsername(), tontineId);
    }

    @PostMapping("/depot")
    @Operation(summary = "Effectuer un dÃ©pÃ´t sur mon compte Ã©pargne")
    public CompteEpargneResponse depot(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody DepotRequest request
    ) {
        return epargneService.depot(principal.getUsername(), request);
    }

    @PostMapping("/retrait")
    @Operation(summary = "Effectuer un retrait de mon compte Ã©pargne")
    public CompteEpargneResponse retrait(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody RetraitRequest request
    ) {
        return epargneService.retrait(principal.getUsername(), request);
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique de mes transactions Ã©pargne â€” tontine courante si tontineId fourni")
    public List<MouvementEpargneResponse> getHistorique(@AuthenticationPrincipal UserDetails principal,
                                                        @RequestParam(required = false) UUID tontineId) {
        return epargneService.getHistorique(principal.getUsername(), tontineId);
    }

    // â”€â”€ Endpoints admin â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/comptes")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Lister tous les comptes Ã©pargne (filtrable par tontine)")
    public List<CompteEpargneResponse> getAllComptes(
            @RequestParam(required = false) UUID tontineId
    ) {
        return epargneService.getAllComptes(tontineId);
    }

    @GetMapping("/comptes/{membreId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Compte Ã©pargne d'un membre")
    public CompteEpargneResponse getCompteByMembre(@PathVariable UUID membreId) {
        return epargneService.getCompteByMembre(membreId);
    }

    @GetMapping("/comptes/{membreId}/historique")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Historique Ã©pargne d'un membre")
    public List<MouvementEpargneResponse> getHistoriqueByMembre(@PathVariable UUID membreId) {
        return epargneService.getHistoriqueByMembre(membreId);
    }

    @PostMapping("/distribuer-interets")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Distribuer les intÃ©rÃªts sur tous les comptes d'une tontine")
    public Map<String, Object> distribuerInterets(@RequestParam UUID tontineId) {
        int nb = epargneService.distribuerInterets(tontineId);
        return Map.of("comptesCredites", nb);
    }
}

