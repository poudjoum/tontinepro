package com.tontinepro.tontinepro_backend.api.document;

import com.tontinepro.tontinepro_backend.api.document.dto.DocumentResponse;
import com.tontinepro.tontinepro_backend.domain.document.Document;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "TÃ©lÃ©charger un document (membre sur son propre profil, ou admin)")
    public DocumentResponse telecharger(
            @RequestParam UUID membreId,
            @RequestParam Document.TypeDocument typeDocument,
            @RequestParam("fichier") MultipartFile fichier,
            @AuthenticationPrincipal UserDetails principal
    ) throws IOException {
        return documentService.telecharger(membreId, typeDocument, fichier, principal.getUsername());
    }

    @GetMapping("/membre/{membreId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Lister les documents d'un membre (Admin / PrÃ©sident / SecrÃ©taire)")
    public List<DocumentResponse> listerParMembre(@PathVariable UUID membreId) {
        return documentService.listerParMembre(membreId);
    }

    @GetMapping("/mes-documents")
    @Operation(summary = "Mes documents")
    public List<DocumentResponse> mesDocuments(@AuthenticationPrincipal UserDetails principal) {
        return documentService.mesDocuments(principal.getUsername());
    }

    @GetMapping("/{id}/fichier")
    @Operation(summary = "TÃ©lÃ©charger le fichier d'un document (propriÃ©taire ou Admin)")
    public ResponseEntity<Resource> telechargerFichier(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) throws IOException {
        Resource resource = documentService.telechargerFichier(id, principal.getUsername());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un document")
    public void supprimer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) throws IOException {
        documentService.supprimer(id, principal.getUsername());
    }
}

