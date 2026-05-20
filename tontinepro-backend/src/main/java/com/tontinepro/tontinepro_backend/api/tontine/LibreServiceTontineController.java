package com.tontinepro.tontinepro_backend.api.tontine;

import com.tontinepro.tontinepro_backend.api.auth.dto.AuthResponse;
import com.tontinepro.tontinepro_backend.api.tontine.dto.CreerMaTontineRequest;
import com.tontinepro.tontinepro_backend.api.tontine.dto.CreerTontineAvecCompteRequest;
import com.tontinepro.tontinepro_backend.api.tontine.dto.TontineResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tontines")
@RequiredArgsConstructor
@Tag(name = "Tontines")
public class LibreServiceTontineController {

    private final LibreServiceTontineService libreService;

    /**
     * Création libre-service (public) — crée un compte + une tontine en une étape.
     * Le créateur devient automatiquement Secrétaire général.
     */
    @PostMapping("/creer-avec-compte")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une tontine et un compte en une étape (public — sans compte existant)")
    public AuthResponse creerAvecCompte(@Valid @RequestBody CreerTontineAvecCompteRequest request) {
        return libreService.creerAvecCompte(request);
    }

    /**
     * Création libre-service (authentifié) — pour un utilisateur déjà connecté sans profil.
     * Le créateur devient automatiquement Secrétaire général.
     */
    @PostMapping("/creer-ma-tontine")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer sa propre tontine (utilisateur connecté sans profil membre)")
    public TontineResponse creerMaTontine(
            @Valid @RequestBody CreerMaTontineRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return libreService.creerMaTontine(principal.getUsername(), request);
    }
}
