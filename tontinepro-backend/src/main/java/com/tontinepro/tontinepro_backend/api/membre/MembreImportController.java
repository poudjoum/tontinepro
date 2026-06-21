package com.tontinepro.tontinepro_backend.api.membre;

import com.tontinepro.tontinepro_backend.api.membre.dto.MembreImportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/membres/import")
@RequiredArgsConstructor
@Tag(name = "Import membres")
public class MembreImportController {

    private final MembreImportService         importService;
    private final MembreImportTemplateBuilder templateBuilder;

    @GetMapping("/modele")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Télécharger le modèle Excel d'importation des membres")
    public ResponseEntity<byte[]> modele(@RequestParam(required = false) UUID tontineId) {
        String nom = tontineId != null ? importService.nomTontine(tontineId) : null;
        byte[] contenu = templateBuilder.build(nom);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"modele_import_membres.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenu);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Importer des membres depuis un fichier Excel")
    public MembreImportResponse importer(
            @RequestParam UUID tontineId,
            @RequestParam("fichier") MultipartFile fichier,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return importService.importer(tontineId, fichier, principal.getUsername());
    }
}
