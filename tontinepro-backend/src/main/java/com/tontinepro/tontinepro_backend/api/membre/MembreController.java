package com.tontinepro.tontinepro_backend.api.membre;

import com.tontinepro.tontinepro_backend.api.membre.dto.CreateMembreRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.InscriptionDirecteRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.MembreResponse;
import com.tontinepro.tontinepro_backend.api.membre.dto.UpdateMembreFonctionRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.UpdateMembreStatutRequest;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
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
@RequestMapping("/api/v1/membres")
@RequiredArgsConstructor
@Tag(name = "Membres")
public class MembreController {

    private final MembreService membreService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inscrire un membre")
    public MembreResponse create(@Valid @RequestBody CreateMembreRequest request) {
        return membreService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les membres (filtrables par tontine et statut)")
    public List<MembreResponse> list(
            @RequestParam(required = false) UUID tontineId,
            @RequestParam(required = false) Membre.Statut statut
    ) {
        return membreService.list(tontineId, statut);
    }

    @GetMapping("/me")
    @Operation(summary = "Mon profil membre")
    public MembreResponse getMe(@AuthenticationPrincipal UserDetails principal) {
        return membreService.getMe(principal.getUsername());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Détails d'un membre")
    public MembreResponse getById(@PathVariable UUID id) {
        return membreService.getById(id);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier le statut d'un membre")
    public MembreResponse updateStatut(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMembreStatutRequest request
    ) {
        return membreService.updateStatut(id, request);
    }

    @PostMapping("/inscription-directe")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inscrire directement un nouveau membre (crée le compte utilisateur + le profil membre)")
    public MembreResponse inscrireDirectement(@Valid @RequestBody InscriptionDirecteRequest request) {
        return membreService.inscrireDirectement(request);
    }

    @PatchMapping("/{id}/fonction")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier la fonction d'un membre dans le bureau")
    public MembreResponse updateFonction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMembreFonctionRequest request
    ) {
        return membreService.updateFonction(id, request);
    }
}
