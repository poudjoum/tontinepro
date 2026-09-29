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

    // Dépôts et retraits sont saisis par le bureau, à l'encaissement ou au
    // décaissement réel : le membre ne crédite pas lui-même son épargne.
    @PreAuthorize("@sec.bureauEncaisse(authentication.name, @tontineDe.membre(#membreId))")
    @PostMapping("/comptes/{membreId}/depot")
    @Operation(summary = "Enregistrer un dépôt sur le compte épargne d'un membre (bureau)")
    public CompteEpargneResponse depot(
            @PathVariable UUID membreId,
            @Valid @RequestBody DepotRequest request
    ) {
        return epargneService.depot(membreId, request);
    }

    @PreAuthorize("@sec.bureauEncaisse(authentication.name, @tontineDe.membre(#membreId))")
    @PostMapping("/comptes/{membreId}/retrait")
    @Operation(summary = "Enregistrer un retrait sur le compte épargne d'un membre (bureau)")
    public CompteEpargneResponse retrait(
            @PathVariable UUID membreId,
            @Valid @RequestBody RetraitRequest request
    ) {
        return epargneService.retrait(membreId, request);
    }

    @GetMapping("/historique")
    @Operation(summary = "Historique de mes transactions Ã©pargne â€” tontine courante si tontineId fourni")
    public List<MouvementEpargneResponse> getHistorique(@AuthenticationPrincipal UserDetails principal,
                                                        @RequestParam(required = false) UUID tontineId) {
        return epargneService.getHistorique(principal.getUsername(), tontineId);
    }

    // â”€â”€ Endpoints admin â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/comptes")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId)")
    @Operation(summary = "Lister tous les comptes Ã©pargne (filtrable par tontine)")
    public List<CompteEpargneResponse> getAllComptes(
            @RequestParam(required = false) UUID tontineId
    ) {
        return epargneService.getAllComptes(tontineId);
    }

    @GetMapping("/comptes/{membreId}")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.membre(#membreId))")
    @Operation(summary = "Compte Ã©pargne d'un membre")
    public CompteEpargneResponse getCompteByMembre(@PathVariable UUID membreId) {
        return epargneService.getCompteByMembre(membreId);
    }

    @GetMapping("/comptes/{membreId}/historique")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.membre(#membreId))")
    @Operation(summary = "Historique Ã©pargne d'un membre")
    public List<MouvementEpargneResponse> getHistoriqueByMembre(@PathVariable UUID membreId) {
        return epargneService.getHistoriqueByMembre(membreId);
    }

    @PostMapping("/distribuer-interets")
    @PreAuthorize("@sec.gere(authentication.name, #tontineId)")
    @Operation(summary = "Distribuer les intÃ©rÃªts sur tous les comptes d'une tontine")
    public Map<String, Object> distribuerInterets(@RequestParam UUID tontineId) {
        int nb = epargneService.distribuerInterets(tontineId);
        return Map.of("comptesCredites", nb);
    }
}

