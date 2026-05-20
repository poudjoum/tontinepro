package com.tontinepro.tontinepro_backend.api.documenttontine;

import com.tontinepro.tontinepro_backend.api.documenttontine.dto.DocumentTontineResponse;
import com.tontinepro.tontinepro_backend.domain.documenttontine.DocumentTontine;
import com.tontinepro.tontinepro_backend.domain.documenttontine.DocumentTontineRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tontines/{tontineId}/documents-officiels")
@RequiredArgsConstructor
@Tag(name = "Documents officiels tontine")
public class DocumentTontineController {

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    private final DocumentTontineRepository docRepo;
    private final TontineRepository tontineRepository;

    /** Public — lister les documents officiels */
    @GetMapping
    @Operation(summary = "Lister les documents officiels d'une tontine (public)")
    public List<DocumentTontineResponse> lister(@PathVariable UUID tontineId) {
        return docRepo.findAllByTontineId(tontineId)
                .stream().map(DocumentTontineResponse::from).toList();
    }

    /** Public — télécharger un document officiel */
    @GetMapping("/{docId}/fichier")
    @Operation(summary = "Télécharger un document officiel (public)")
    public ResponseEntity<Resource> fichier(
            @PathVariable UUID tontineId,
            @PathVariable UUID docId
    ) throws IOException {
        DocumentTontine doc = docRepo.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable"));
        Path path = Paths.get(doc.getCheminStockage());
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNomFichier() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, doc.getContentType())
                .body(resource);
    }

    /** Secrétaire/Président — uploader un document officiel */
    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Uploader un document officiel")
    public DocumentTontineResponse upload(
            @PathVariable UUID tontineId,
            @RequestParam DocumentTontine.TypeDocument typeDocument,
            @RequestParam("fichier") MultipartFile fichier
    ) throws IOException {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable"));

        String ext = fichier.getOriginalFilename() != null
                ? fichier.getOriginalFilename().substring(fichier.getOriginalFilename().lastIndexOf('.'))
                : ".pdf";

        Path dir = Paths.get(uploadDir, "tontine-" + tontineId);
        Files.createDirectories(dir);
        Path dest = dir.resolve(UUID.randomUUID() + ext);
        Files.copy(fichier.getInputStream(), dest);

        DocumentTontine doc = DocumentTontine.builder()
                .tontine(tontine)
                .typeDocument(typeDocument)
                .nomFichier(fichier.getOriginalFilename())
                .cheminStockage(dest.toString())
                .tailleOctets(fichier.getSize())
                .contentType(fichier.getContentType() != null ? fichier.getContentType() : "application/pdf")
                .build();

        return DocumentTontineResponse.from(docRepo.save(doc));
    }

    /** Secrétaire/Président — supprimer */
    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Supprimer un document officiel")
    public void supprimer(@PathVariable UUID tontineId, @PathVariable UUID docId) throws IOException {
        DocumentTontine doc = docRepo.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable"));
        Files.deleteIfExists(Paths.get(doc.getCheminStockage()));
        docRepo.delete(doc);
    }
}
