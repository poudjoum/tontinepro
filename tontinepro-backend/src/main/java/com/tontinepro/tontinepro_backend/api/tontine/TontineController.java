package com.tontinepro.tontinepro_backend.api.tontine;

import com.tontinepro.tontinepro_backend.api.tontine.dto.CreateTontineRequest;
import com.tontinepro.tontinepro_backend.api.tontine.dto.TontineResponse;
import com.tontinepro.tontinepro_backend.api.tontine.dto.UpdateTontineConfigRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tontines")
@RequiredArgsConstructor
@Tag(name = "Tontines")
public class TontineController {

    private final TontineService tontineService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une tontine")
    public TontineResponse create(@Valid @RequestBody CreateTontineRequest request) {
        return tontineService.create(request);
    }

    /** Public — tontines visibles pour les candidats à l'adhésion */
    @GetMapping("/publiques")
    @Operation(summary = "Lister les tontines disponibles (public, sans authentification)")
    public List<TontineResponse> listPubliques() {
        return tontineService.listPubliques();
    }

    @GetMapping
    @Operation(summary = "Lister les tontines actives (authentifié)")
    public List<TontineResponse> listActive() {
        return tontineService.listActive();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'une tontine")
    public TontineResponse getById(@PathVariable UUID id) {
        return tontineService.getById(id);
    }

    @PatchMapping("/{id}/config")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Modifier la configuration d'une tontine")
    public TontineResponse updateConfig(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTontineConfigRequest request
    ) {
        return tontineService.updateConfig(id, request);
    }
}
