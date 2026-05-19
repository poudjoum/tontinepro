package com.tontinepro.tontinepro_backend.api.document;

import com.tontinepro.tontinepro_backend.api.document.dto.DocumentResponse;
import com.tontinepro.tontinepro_backend.domain.document.Document;
import com.tontinepro.tontinepro_backend.domain.document.DocumentRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    @Value("${app.upload.dir:uploads/documents}")
    private String uploadDir;

    private final DocumentRepository documentRepository;
    private final MembreRepository membreRepository;

    @Transactional
    public DocumentResponse telecharger(UUID membreId, Document.TypeDocument type,
                                        MultipartFile fichier, String uploader) throws IOException {
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + membreId));

        String contentType = fichier.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new IllegalArgumentException("Seuls les fichiers PDF et images sont acceptés");
        }
        if (fichier.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Fichier trop volumineux (max 10 Mo)");
        }

        String ext = fichier.getOriginalFilename() != null
                ? fichier.getOriginalFilename().substring(fichier.getOriginalFilename().lastIndexOf('.'))
                : ".bin";

        String nomStockage = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadDir, membreId.toString());
        Files.createDirectories(dir);
        Path dest = dir.resolve(nomStockage);
        Files.copy(fichier.getInputStream(), dest);

        Document doc = Document.builder()
                .membre(membre)
                .typeDocument(type)
                .nomFichier(fichier.getOriginalFilename())
                .cheminStockage(dest.toString())
                .tailleOctets(fichier.getSize())
                .contentType(contentType)
                .build();

        return DocumentResponse.from(documentRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listerParMembre(UUID membreId) {
        return documentRepository.findAllByMembreId(membreId)
                .stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> mesDocuments(String email) {
        Membre membre = membreRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));
        return documentRepository.findAllByMembreId(membre.getId())
                .stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Resource telechargerFichier(UUID documentId, String email) throws IOException {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable : " + documentId));

        // Users without a Membre profile are admins — they can access all documents.
        // Members can only access their own documents.
        membreRepository.findByUserEmail(email).ifPresent(membre -> {
            if (!membre.getId().equals(doc.getMembre().getId())) {
                throw new IllegalArgumentException("Accès refusé à ce document");
            }
        });

        Path path = Paths.get(doc.getCheminStockage());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) {
            throw new IllegalArgumentException("Fichier introuvable sur le serveur");
        }
        return resource;
    }

    @Transactional
    public void supprimer(UUID documentId, String email) throws IOException {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable : " + documentId));

        Membre membre = membreRepository.findByUserEmail(email).orElse(null);
        if (membre == null || !membre.getId().equals(doc.getMembre().getId())) {
            throw new IllegalArgumentException("Vous ne pouvez supprimer que vos propres documents");
        }

        Files.deleteIfExists(Paths.get(doc.getCheminStockage()));
        documentRepository.delete(doc);
    }
}
