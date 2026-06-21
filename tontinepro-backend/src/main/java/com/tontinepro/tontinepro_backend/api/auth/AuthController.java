package com.tontinepro.tontinepro_backend.api.auth;

import com.tontinepro.tontinepro_backend.api.auth.dto.*;
import com.tontinepro.tontinepro_backend.api.auth.dto.ClaimerCompteRequest;
import com.tontinepro.tontinepro_backend.api.superadmin.SuperAdminService;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.DemandeReinitMdpResponse;
import com.tontinepro.tontinepro_backend.api.superadmin.dto.SoumettreReinitMdpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService       authService;
    private final TwoFaService      twoFaService;
    private final SuperAdminService superAdminService;

    // ── Auth de base ─────────────────────────────────────────────────────

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer un compte")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Retourne soit un {@link AuthResponse} complet (2FA désactivée),
     * soit un {@link TwoFaChallengeResponse} avec un ticket éphémère (2FA activée).
     */
    @PostMapping("/login")
    @Operation(summary = "Se connecter")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
        return switch (authService.login(request)) {
            case LoginResult.Authenticated a ->
                    ResponseEntity.ok(a.auth());
            case LoginResult.TwoFaRequired r ->
                    ResponseEntity.ok(new TwoFaChallengeResponse(true, r.ticket(), r.email()));
        };
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouveler le token d'accès")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Se déconnecter")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails principal) {
        authService.logout(principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/activer-compte")
    @Operation(summary = "Activer son compte via numéro de téléphone (membres pré-inscrits par un admin)")
    public AuthResponse activerCompte(@Valid @RequestBody ClaimerCompteRequest request) {
        return authService.activerCompte(request);
    }

    @PostMapping("/changer-mot-de-passe")
    @Operation(summary = "Changer son mot de passe temporaire (1ère connexion d'un membre importé)")
    public AuthResponse changerMotDePasse(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ChangerMotDePasseRequest request
    ) {
        return authService.changerMotDePasse(principal.getUsername(), request.nouveauMotDePasse());
    }

    // ── 2FA ──────────────────────────────────────────────────────────────

    @PostMapping("/2fa/activer")
    @Operation(summary = "Initialiser la 2FA — génère le secret et le QR code")
    public TwoFaSetupResponse activer2fa(@AuthenticationPrincipal UserDetails principal) {
        return twoFaService.setup(principal.getUsername());
    }

    @PostMapping("/2fa/confirmer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirmer l'activation 2FA avec le premier code TOTP")
    public ResponseEntity<Void> confirmer2fa(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ConfirmerTwoFaRequest request
    ) {
        twoFaService.confirmer(principal.getUsername(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/2fa/valider")
    @Operation(summary = "Valider le code TOTP après login — retourne les tokens d'accès")
    public AuthResponse valider2fa(@Valid @RequestBody ValiderTwoFaRequest request) {
        return authService.loginWith2fa(request);
    }

    @PostMapping("/2fa/desactiver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Désactiver la 2FA (code TOTP courant requis)")
    public ResponseEntity<Void> desactiver2fa(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ConfirmerTwoFaRequest request
    ) {
        twoFaService.desactiver(principal.getUsername(), request.code());
        return ResponseEntity.noContent().build();
    }

    // ── Réinitialisation mot de passe (lien envoyé par le super admin) ───

    // ── Demande de réinitialisation de mot de passe (public) ─────────────────────

    @PostMapping("/mot-de-passe-oublie")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Soumettre une demande de réinitialisation de mot de passe (public)")
    public DemandeReinitMdpResponse motDePasseOublie(@Valid @RequestBody SoumettreReinitMdpRequest request) {
        return superAdminService.soumettreDemande(request);
    }

    @GetMapping("/reset-password/verify")
    @Operation(summary = "Vérifier la validité d'un token de réinitialisation (public)")
    public java.util.Map<String, Object> verifierToken(@RequestParam String token) {
        boolean valide = authService.verifierResetToken(token);
        return java.util.Map.of("valide", valide);
    }

    @PostMapping("/reset-password/confirmer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Confirmer le nouveau mot de passe avec le token de réinitialisation (public)")
    public ResponseEntity<Void> confirmerResetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.confirmerResetPassword(request.token(), request.nouveauMotDePasse());
        return ResponseEntity.noContent().build();
    }
}
