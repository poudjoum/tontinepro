package com.tontinepro.tontinepro_backend.api.membre;

import com.tontinepro.tontinepro_backend.api.membre.dto.CreateMembreRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.InscriptionDirecteRequest;
import com.tontinepro.tontinepro_backend.api.membre.dto.MembreResponse;
import com.tontinepro.tontinepro_backend.api.membre.dto.ReinitialiserMembresResponse;
import com.tontinepro.tontinepro_backend.api.membre.dto.SuiviMoisResponse;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/membres")
@RequiredArgsConstructor
@Tag(name = "Membres")
public class MembreController {

    private final MembreService membreService;
    private final SuiviService suiviService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Inscrire un membre")
    public MembreResponse create(@Valid @RequestBody CreateMembreRequest request) {
        return membreService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Lister les membres (filtrables par tontine et statut)")
    public List<MembreResponse> list(
            @RequestParam(required = false) UUID tontineId,
            @RequestParam(required = false) Membre.Statut statut,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return membreService.list(tontineId, statut, principal.getUsername());
    }

    @GetMapping("/me")
    @Operation(summary = "Mon profil membre (dans la tontine courante si tontineId fourni)")
    public MembreResponse getMe(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam(required = false) UUID tontineId) {
        return membreService.getMe(principal.getUsername(), tontineId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "DÃ©tails d'un membre")
    public MembreResponse getById(@PathVariable UUID id) {
        return membreService.getById(id);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Modifier le statut d'un membre")
    public MembreResponse updateStatut(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMembreStatutRequest request
    ) {
        return membreService.updateStatut(id, request);
    }

    @PostMapping("/inscription-directe")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Inscrire directement un nouveau membre (crÃ©e le compte utilisateur + le profil membre)")
    public MembreResponse inscrireDirectement(@Valid @RequestBody InscriptionDirecteRequest request) {
        return membreService.inscrireDirectement(request);
    }

    @PatchMapping("/{id}/fonction")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Modifier la fonction d'un membre dans le bureau")
    public MembreResponse updateFonction(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMembreFonctionRequest request
    ) {
        return membreService.updateFonction(id, request);
    }

    @DeleteMapping("/tontine/{tontineId}/reinitialiser")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réinitialiser les membres d'une tontine sans activité financière (pour réinscription via invitation)")
    public ReinitialiserMembresResponse reinitialiser(@PathVariable UUID tontineId) {
        return membreService.reinitialiserPourInvitation(tontineId);
    }

    @GetMapping("/me/suivi")
    @Operation(summary = "Suivi mensuel du membre connecté (cotisations, épargne, sanctions, prêts)")
    public List<SuiviMoisResponse> getMonSuivi(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) UUID tontineId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        int anneeEffective = annee != null ? annee : LocalDate.now().getYear();
        return suiviService.getSuiviAnnuel(principal.getUsername(), anneeEffective, tontineId);
    }
}

