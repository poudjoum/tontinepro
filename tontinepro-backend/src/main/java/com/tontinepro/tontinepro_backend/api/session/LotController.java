package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.session.dto.AdhererLotRequest;
import com.tontinepro.tontinepro_backend.api.session.dto.SessionLotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/lot")
@RequiredArgsConstructor
@Tag(name = "Tontine à lot")
public class LotController {

    private final LotService lotService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Vue d'une session à lot : adhésions et lots/tours")
    public SessionLotResponse getLot(@PathVariable UUID sessionId) {
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/adherer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Inscrire/mettre à jour la mise mensuelle d'un membre (avant figeage)")
    public SessionLotResponse adherer(@PathVariable UUID sessionId,
                                      @Valid @RequestBody AdhererLotRequest request) {
        lotService.adherer(sessionId, request.membreId(), request.montantMensuel());
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/figer")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Figer manuellement la session (sinon automatique en fin de période d'adhésion)")
    public SessionLotResponse figer(@PathVariable UUID sessionId) {
        lotService.figer(sessionId);
        return lotService.getLotView(sessionId);
    }
}
