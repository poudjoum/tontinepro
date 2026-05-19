package com.tontinepro.tontinepro_backend.api.invitation;

import com.tontinepro.tontinepro_backend.api.auth.dto.AuthResponse;
import com.tontinepro.tontinepro_backend.api.invitation.dto.GenererInvitationRequest;
import com.tontinepro.tontinepro_backend.api.invitation.dto.InvitationResponse;
import com.tontinepro.tontinepro_backend.api.invitation.dto.RejoindreRequest;
import com.tontinepro.tontinepro_backend.api.invitation.dto.StatutInvitationResponse;
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
@RequestMapping("/api/v1/invitation")
@RequiredArgsConstructor
@Tag(name = "Invitation")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("/generer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Générer un lien d'invitation à la tontine")
    public InvitationResponse generer(
            @Valid @RequestBody GenererInvitationRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return invitationService.generer(request, principal.getUsername());
    }

    @GetMapping("/{token}/statut")
    @Operation(summary = "Vérifier la validité d'un lien d'invitation (public)")
    public StatutInvitationResponse statut(@PathVariable String token) {
        return invitationService.statut(token);
    }

    @PostMapping("/{token}/rejoindre")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Rejoindre la tontine via le lien d'invitation (public)")
    public AuthResponse rejoindre(
            @PathVariable String token,
            @Valid @RequestBody RejoindreRequest request
    ) {
        return invitationService.rejoindre(token, request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les invitations")
    public List<InvitationResponse> lister(@RequestParam(required = false) UUID tontineId) {
        return invitationService.lister(tontineId);
    }
}
