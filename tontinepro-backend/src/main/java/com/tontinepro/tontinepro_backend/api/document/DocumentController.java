package com.tontinepro.tontinepro_backend.api.document;

import com.tontinepro.tontinepro_backend.api.document.dto.DocumentResponse;
import com.tontinepro.tontinepro_backend.domain.document.Document;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents membres")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Télécharger un document membre (PDF ou image, max 10 Mo)")
    public DocumentResponse telecharger(
            @RequestParam UUID membreId,
            @RequestParam Document.TypeDocument typeDocument,
            @RequestParam("fichier") MultipartFile fichier,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return documentService.telecharger(membreId, typeDocument, fichier, principal.getUsername());
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Lister les documents d'un membre (Admin / Secrétaire)")
    public List<DocumentResponse> listerParMembre(@PathVariable UUID membreId) {
        return documentService.listerParMembre(membreId);
    }

    @GetMapping("/mes-documents")
    @Operation(summary = "Mes documents")
    public List<DocumentResponse> mesDocuments(@AuthenticationPrincipal UserDetails principal) {
        return documentService.mesDocuments(principal.getUsername());
    }

    @GetMapping("/{id}/fichier")
    @Operation(summary = "Télécharger un fichier document (propriétaire ou Admin)")
    public ResponseEntity<InputStreamResource> telechargerFichier(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        InputStream stream = documentService.telechargerFichier(id, principal.getUsername());
        String contentType = documentService.getContentType(id);
        String nomFichier  = documentService.getNomFichier(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomFichier + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un document")
    public void supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        documentService.supprimer(id, principal.getUsername());
    }
}
