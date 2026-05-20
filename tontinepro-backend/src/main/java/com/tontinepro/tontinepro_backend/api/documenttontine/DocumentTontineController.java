package com.tontinepro.tontinepro_backend.api.documenttontine;

import com.tontinepro.tontinepro_backend.api.documenttontine.dto.DocumentTontineResponse;
import com.tontinepro.tontinepro_backend.domain.documenttontine.DocumentTontine;
import com.tontinepro.tontinepro_backend.domain.documenttontine.DocumentTontineRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.infrastructure.config.MinioConfig;
import com.tontinepro.tontinepro_backend.infrastructure.storage.MinioStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tontines/{tontineId}/documents-officiels")
@RequiredArgsConstructor
@Tag(name = "Documents officiels tontine")
public class DocumentTontineController {

    private final DocumentTontineRepository docRepo;
    private final TontineRepository tontineRepository;
    private final MinioStorageService minioStorage;

    @GetMapping
    @Operation(summary = "Lister les documents officiels d'une tontine (public)")
    public List<DocumentTontineResponse> lister(@PathVariable UUID tontineId) {
        return docRepo.findAllByTontineId(tontineId)
                .stream().map(DocumentTontineResponse::from).toList();
    }

    @GetMapping("/{docId}/fichier")
    @Operation(summary = "Télécharger un document officiel depuis MinIO (public)")
    public ResponseEntity<InputStreamResource> fichier(
            @PathVariable UUID tontineId,
            @PathVariable UUID docId
    ) {
        DocumentTontine doc = docRepo.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable"));

        InputStream stream = minioStorage.telecharger(MinioConfig.BUCKET_TONTINES, doc.getCheminStockage());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNomFichier() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, doc.getContentType())
                .body(new InputStreamResource(stream));
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Publier un document officiel dans MinIO (PDF uniquement)")
    public DocumentTontineResponse upload(
            @PathVariable UUID tontineId,
            @RequestParam DocumentTontine.TypeDocument typeDocument,
            @RequestParam("fichier") MultipartFile fichier
    ) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable"));

        if (fichier.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Fichier trop volumineux (max 10 Mo)");
        }
        String ct = fichier.getContentType();
        if (ct == null || !ct.equals("application/pdf")) {
            throw new IllegalArgumentException("Seuls les fichiers PDF sont acceptés pour les documents officiels");
        }

        String objectKey = minioStorage.stocker(
                MinioConfig.BUCKET_TONTINES,
                "tontines/" + tontineId + "/" + typeDocument.name().toLowerCase(),
                fichier
        );

        DocumentTontine doc = DocumentTontine.builder()
                .tontine(tontine)
                .typeDocument(typeDocument)
                .nomFichier(fichier.getOriginalFilename())
                .cheminStockage(objectKey)
                .tailleOctets(fichier.getSize())
                .contentType("application/pdf")
                .build();

        return DocumentTontineResponse.from(docRepo.save(doc));
    }

    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Supprimer un document officiel")
    public void supprimer(@PathVariable UUID tontineId, @PathVariable UUID docId) {
        DocumentTontine doc = docRepo.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable"));
        minioStorage.supprimer(MinioConfig.BUCKET_TONTINES, doc.getCheminStockage());
        docRepo.delete(doc);
    }
}
