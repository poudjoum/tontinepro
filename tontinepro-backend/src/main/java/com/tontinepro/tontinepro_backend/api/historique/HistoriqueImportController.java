package com.tontinepro.tontinepro_backend.api.historique;

import com.tontinepro.tontinepro_backend.api.historique.dto.ImporterHistoriqueRequest;
import com.tontinepro.tontinepro_backend.api.historique.dto.ImporterHistoriqueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/historique")
@RequiredArgsConstructor
@Tag(name = "Import historique")
public class HistoriqueImportController {

    private final HistoriqueImportService service;

    @PostMapping("/importer")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Importer l'historique d'une session existante (rattrapage)")
    public ImporterHistoriqueResponse importer(
            @PathVariable UUID sessionId,
            @RequestBody ImporterHistoriqueRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return service.importer(sessionId, request, principal.getUsername());
    }
}