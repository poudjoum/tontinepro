package com.tontinepro.tontinepro_backend.api.demande;

import com.tontinepro.tontinepro_backend.api.demande.dto.DemandeResponse;
import com.tontinepro.tontinepro_backend.api.demande.dto.RejeterDemandeRequest;
import com.tontinepro.tontinepro_backend.api.demande.dto.SoumettreDemandeRequest;
import com.tontinepro.tontinepro_backend.domain.demande.DemandeAdhesion;
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
@RequiredArgsConstructor
@Tag(name = "Demandes d'adhésion")
public class DemandeController {

    private final DemandeService demandeService;

    /** Public — soumettre une demande sans être connecté */
    @PostMapping("/api/v1/tontines/{tontineId}/demandes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Soumettre une demande d'adhésion (public)")
    public DemandeResponse soumettre(
            @PathVariable UUID tontineId,
            @Valid @RequestBody SoumettreDemandeRequest request
    ) {
        return demandeService.soumettre(tontineId, request);
    }

    /** Secrétaire/Président — lister les demandes */
    @GetMapping("/api/v1/demandes")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Lister les demandes d'adhésion")
    public List<DemandeResponse> lister(
            @RequestParam(required = false) UUID tontineId,
            @RequestParam(required = false) DemandeAdhesion.Statut statut
    ) {
        return demandeService.lister(tontineId, statut);
    }

    /** Secrétaire/Président — approuver */
    @PostMapping("/api/v1/demandes/{id}/approuver")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Approuver une demande (crée le compte + le profil membre)")
    public DemandeResponse approuver(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return demandeService.approuver(id, principal.getUsername());
    }

    /** Secrétaire/Président — rejeter avec motif */
    @PostMapping("/api/v1/demandes/{id}/rejeter")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Rejeter une demande avec motif")
    public DemandeResponse rejeter(
            @PathVariable UUID id,
            @Valid @RequestBody RejeterDemandeRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return demandeService.rejeter(id, request, principal.getUsername());
    }
}
