package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.AideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.DemandeAideRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.RejeterAideRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.ValiderAideRequest;
import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/aides")
@RequiredArgsConstructor
@Tag(name = "Aides Mutuelles")
public class AideController {

    private final AideService aideService;

    @PostMapping("/demandes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Soumettre une demande d'aide")
    public AideResponse soumettreDemande(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody DemandeAideRequest request
    ) {
        return aideService.soumettreDemande(principal.getUsername(), request);
    }

    @GetMapping("/mes-demandes")
    @Operation(summary = "Mes demandes d'aide")
    public List<AideResponse> getMesDemandes(@AuthenticationPrincipal UserDetails principal) {
        return aideService.getMesDemandes(principal.getUsername());
    }

    @GetMapping("/demandes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les demandes d'aide (filtrables par membre, tontine, statut)")
    public List<AideResponse> list(
            @RequestParam(required = false) UUID membreId,
            @RequestParam(required = false) UUID tontineId,
            @RequestParam(required = false) Aide.Statut statut
    ) {
        return aideService.list(membreId, tontineId, statut);
    }

    @GetMapping("/demandes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Détails d'une demande d'aide")
    public AideResponse getById(@PathVariable UUID id) {
        return aideService.getById(id);
    }

    @PatchMapping("/demandes/{id}/valider")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approuver une demande d'aide")
    public AideResponse valider(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ValiderAideRequest request
    ) {
        return aideService.valider(id, principal.getUsername(), request);
    }

    @PatchMapping("/demandes/{id}/rejeter")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rejeter une demande d'aide")
    public AideResponse rejeter(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody RejeterAideRequest request
    ) {
        return aideService.rejeter(id, principal.getUsername(), request);
    }

    @PatchMapping("/demandes/{id}/payer")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marquer une aide comme payée")
    public AideResponse marquerPayee(@PathVariable UUID id) {
        return aideService.marquerPayee(id);
    }
}