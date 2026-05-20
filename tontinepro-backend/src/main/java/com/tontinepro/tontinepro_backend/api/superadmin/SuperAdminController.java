package com.tontinepro.tontinepro_backend.api.superadmin;

import com.tontinepro.tontinepro_backend.api.superadmin.dto.ConfigurerRedevanceRequest;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.TontinePlatformeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin — Gestion plateforme")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/tontines")
    @Operation(summary = "Lister toutes les tontines de la plateforme")
    public List<TontinePlatformeResponse> listerTontines() {
        return superAdminService.listerTontines();
    }

    @PatchMapping("/tontines/{id}/actif")
    @Operation(summary = "Activer ou désactiver une tontine")
    public TontinePlatformeResponse toggleActif(
            @PathVariable UUID id,
            @RequestParam boolean actif
    ) {
        return superAdminService.toggleActif(id, actif);
    }

    @PutMapping("/tontines/{id}/redevance")
    @Operation(summary = "Configurer la redevance mensuelle / annuelle d'une tontine")
    public TontinePlatformeResponse configurerRedevance(
            @PathVariable UUID id,
            @Valid @RequestBody ConfigurerRedevanceRequest request
    ) {
        return superAdminService.configurerRedevance(id, request);
    }

    @PostMapping("/tontines/{id}/reset-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envoyer un lien de réinitialisation de mot de passe au Président et au Secrétaire")
    public Map<String, Object> reinitialiserMotDePasse(@PathVariable UUID id) {
        int nbEnvois = superAdminService.reinitialiserMotDePasse(id);
        return Map.of("message", nbEnvois + " lien(s) de réinitialisation envoyé(s)", "nbEnvois", nbEnvois);
    }
}
