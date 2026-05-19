package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.session.dto.*;
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
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions de tontine")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "CrÃ©er une nouvelle session de tontine")
    public SessionResponse creerSession(@Valid @RequestBody CreerSessionRequest request) {
        return sessionService.creerSession(request);
    }

    @GetMapping
    @Operation(summary = "Lister les sessions d'une tontine")
    public List<SessionResponse> listerSessions(@RequestParam UUID tontineId) {
        return sessionService.listerSessions(tontineId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "DÃ©tails d'une session avec la liste ordonnÃ©e des bÃ©nÃ©ficiaires")
    public SessionResponse getById(@PathVariable UUID id) {
        return sessionService.getById(id);
    }

    @PatchMapping("/{id}/prochaine-date")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Mettre Ã  jour la prochaine date de tontine (mode DATE_MANUELLE)")
    public SessionResponse mettreAJourProchainDate(
            @PathVariable UUID id,
            @Valid @RequestBody MiseAJourDateRequest request) {
        return sessionService.mettreAJourProchainDate(id, request);
    }

    @PatchMapping("/{id}/beneficiaires/reordonner")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "RÃ©ordonner la liste des bÃ©nÃ©ficiaires d'une session")
    public SessionResponse reordonnerBeneficiaires(
            @PathVariable UUID id,
            @Valid @RequestBody ReordonnerBeneficiairesRequest request) {
        return sessionService.reordonnerBeneficiaires(id, request);
    }

    @PostMapping("/{id}/valider-benefice/{ordreBeneficiaireId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Valider le bÃ©nÃ©fice d'un membre dans la session")
    public SessionResponse validerBenefice(
            @PathVariable UUID id,
            @PathVariable UUID ordreBeneficiaireId,
            @Valid @RequestBody ValiderBeneficeRequest request) {
        return sessionService.validerBenefice(id, ordreBeneficiaireId, request);
    }
}

