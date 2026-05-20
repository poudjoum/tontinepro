package com.tontinepro.tontinepro_backend.api.profil;

import com.tontinepro.tontinepro_backend.api.membre.dto.MembreResponse;
import com.tontinepro.tontinepro_backend.api.profil.dto.ChangerMotDePasseRequest;
import com.tontinepro.tontinepro_backend.api.profil.dto.UpdateProfilRequest;
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
@RequestMapping("/api/v1/profil")
@RequiredArgsConstructor
@Tag(name = "Profil")
public class ProfilController {

    private final ProfilService profilService;

    @PatchMapping("/me")
    @Operation(summary = "Mettre à jour son profil (nom, prénom, téléphone)")
    public MembreResponse mettreAJour(
            @RequestBody UpdateProfilRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return profilService.mettreAJourProfil(principal.getUsername(), request);
    }

    @PostMapping("/me/changer-mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Changer son mot de passe (mot de passe actuel requis)")
    public ResponseEntity<Void> changerMotDePasse(
            @Valid @RequestBody ChangerMotDePasseRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        profilService.changerMotDePasse(principal.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
