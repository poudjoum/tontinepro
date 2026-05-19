package com.tontinepro.tontinepro_backend.api.sanction;

import com.tontinepro.tontinepro_backend.api.sanction.dto.CreerSanctionRequest;
import com.tontinepro.tontinepro_backend.api.sanction.dto.SanctionResponse;
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
@RequestMapping("/api/v1/sanctions")
@RequiredArgsConstructor
@Tag(name = "Sanctions")
public class SanctionController {

    private final SanctionService sanctionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE') or @sec.isCenseur(authentication.name)")
    @Operation(summary = "Créer une sanction (Censeur / Secrétaire / Président)")
    public SanctionResponse creer(@Valid @RequestBody CreerSanctionRequest request) {
        return sanctionService.creer(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE') or @sec.isCenseur(authentication.name)")
    @Operation(summary = "Lister les sanctions d'une tontine")
    public List<SanctionResponse> lister(
            @RequestParam UUID tontineId,
            @RequestParam(required = false) Boolean payee
    ) {
        return sanctionService.listerParTontine(tontineId, payee);
    }

    @GetMapping("/mes-sanctions")
    @Operation(summary = "Mes sanctions")
    public List<SanctionResponse> mesSanctions(@AuthenticationPrincipal UserDetails principal) {
        return sanctionService.mesSanctions(principal.getUsername());
    }

    @PatchMapping("/{id}/payer")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Marquer une sanction comme payée (Secrétaire / Président)")
    public SanctionResponse marquerPayee(@PathVariable UUID id) {
        return sanctionService.marquerPayee(id);
    }
}
