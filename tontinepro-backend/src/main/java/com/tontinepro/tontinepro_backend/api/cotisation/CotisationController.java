package com.tontinepro.tontinepro_backend.api.cotisation;

import com.tontinepro.tontinepro_backend.api.cotisation.dto.CotisationResponse;
import com.tontinepro.tontinepro_backend.api.cotisation.dto.CreateCotisationRequest;
import com.tontinepro.tontinepro_backend.api.cotisation.dto.EnregistrerPaiementRequest;
import com.tontinepro.tontinepro_backend.api.cotisation.dto.ModifierCotisationRequest;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
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
@RequestMapping("/api/v1/cotisations")
@RequiredArgsConstructor
@Tag(name = "Cotisations")
public class CotisationController {

    private final CotisationService cotisationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "CrÃ©er une cotisation pour un membre")
    public CotisationResponse create(@Valid @RequestBody CreateCotisationRequest request) {
        return cotisationService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Lister les cotisations (filtrables par membre, tontine, pÃ©riode, statut) — cloisonné par tontine du compte")
    public List<CotisationResponse> list(
            @RequestParam(required = false) UUID membreId,
            @RequestParam(required = false) UUID tontineId,
            @RequestParam(required = false) Short mois,
            @RequestParam(required = false) Short annee,
            @RequestParam(required = false) Cotisation.Statut statut,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return cotisationService.list(membreId, tontineId, mois, annee, statut, principal.getUsername());
    }

    @GetMapping("/me")
    @Operation(summary = "Mes cotisations (filtrables par tontine courante)")
    public List<CotisationResponse> getMe(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) UUID tontineId) {
        return cotisationService.getMe(principal.getUsername(), tontineId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "DÃ©tails d'une cotisation")
    public CotisationResponse getById(@PathVariable UUID id) {
        return cotisationService.getById(id);
    }

    @PatchMapping("/{id}/paiement")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Enregistrer le paiement d'une cotisation")
    public CotisationResponse enregistrerPaiement(
            @PathVariable UUID id,
            @RequestBody EnregistrerPaiementRequest request
    ) {
        return cotisationService.enregistrerPaiement(id, request);
    }

    @PatchMapping("/{id}/retard")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Marquer une cotisation en retard")
    public CotisationResponse marquerEnRetard(@PathVariable UUID id) {
        return cotisationService.marquerEnRetard(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Corriger une cotisation (montants, statut, référence, date) — tous les champs sont optionnels")
    public CotisationResponse modifier(
            @PathVariable UUID id,
            @RequestBody ModifierCotisationRequest request) {
        return cotisationService.modifier(id, request);
    }
}

