package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.session.dto.AdhererLotRequest;
import com.tontinepro.tontinepro_backend.api.session.dto.CompositionLotsResponse;
import com.tontinepro.tontinepro_backend.api.session.dto.FigerLotRequest;
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
    private final SessionService sessionService;

    @GetMapping
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Vue d'une session à lot : adhésions et lots/tours")
    public SessionLotResponse getLot(@PathVariable UUID sessionId) {
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/adherer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Inscrire/mettre à jour la mise mensuelle d'un membre (avant figeage)")
    public SessionLotResponse adherer(@PathVariable UUID sessionId,
                                      @Valid @RequestBody AdhererLotRequest request) {
        lotService.adherer(sessionId, request.membreId(), request.montantMensuel());
        return lotService.getLotView(sessionId);
    }

    @GetMapping("/composition")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Proposer une composition des lots (combinaisons exactes), sans l'enregistrer")
    public CompositionLotsResponse composition(@PathVariable UUID sessionId) {
        return lotService.apercu(sessionId);
    }

    @PostMapping("/parts/{partId}/verser")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Verser sa part à un co-bénéficiaire (retenue du fond de caisse incluse)")
    public SessionLotResponse verserPart(@PathVariable UUID sessionId, @PathVariable UUID partId) {
        var tour = lotService.verserPart(sessionId, partId);
        if (tour.isBeneficie()) {
            sessionService.envoyerRapportTour(sessionId, tour.getId());   // dernière part : tour clos
        }
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/parts/{partId}/annuler-versement")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Annuler le versement d'une part (correction)")
    public SessionLotResponse annulerVersementPart(@PathVariable UUID sessionId, @PathVariable UUID partId) {
        lotService.annulerVersementPart(sessionId, partId);
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/figer")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Figer la session avec la composition du bureau (ou automatique si absente)")
    public SessionLotResponse figer(@PathVariable UUID sessionId,
                                    @Valid @RequestBody(required = false) FigerLotRequest request) {
        lotService.figer(sessionId, request);
        return lotService.getLotView(sessionId);
    }
}
