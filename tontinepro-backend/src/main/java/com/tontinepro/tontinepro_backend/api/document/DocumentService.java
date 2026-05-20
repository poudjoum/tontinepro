package com.tontinepro.tontinepro_backend.api.document;

import com.tontinepro.tontinepro_backend.api.document.dto.DocumentResponse;
import com.tontinepro.tontinepro_backend.domain.document.Document;
import com.tontinepro.tontinepro_backend.domain.document.DocumentRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import com.tontinepro.tontinepro_backend.infrastructure.config.MinioConfig;
import com.tontinepro.tontinepro_backend.infrastructure.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final MinioStorageService minioStorage;

    @Transactional
    public DocumentResponse telecharger(UUID membreId, Document.TypeDocument type,
                                        MultipartFile fichier, String uploader) {
        Membre membre = membreRepository.findById(membreId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable : " + membreId));

        String contentType = fichier.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new IllegalArgumentException("Seuls les fichiers PDF et images sont acceptés");
        }
        if (fichier.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Fichier trop volumineux (max 10 Mo)");
        }

        // Vérifier droits : propriétaire ou gestionnaire (ADMIN/SECRETAIRE)
        User upUser = userRepository.findByEmail(uploader)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        boolean isGestionnaire = upUser.getRole() == User.Role.ADMIN
                || upUser.getRole() == User.Role.SECRETAIRE;
        if (!isGestionnaire) {
            boolean isOwner = membreRepository.findAllByUserEmail(uploader).stream()
                    .anyMatch(m -> m.getId().equals(membreId));
            if (!isOwner) {
                throw new IllegalArgumentException("Vous ne pouvez télécharger que vos propres documents");
            }
        }

        // Supprimer l'ancienne version du même type s'il en existe une
        documentRepository.findAllByMembreIdAndTypeDocument(membreId, type).forEach(old -> {
            try { minioStorage.supprimer(MinioConfig.BUCKET_MEMBRES, old.getCheminStockage()); } catch (Exception ignored) {}
            documentRepository.delete(old);
        });

        String objectKey = minioStorage.stocker(
                MinioConfig.BUCKET_MEMBRES,
                "membres/" + membreId + "/" + type.name().toLowerCase(),
                fichier
        );

        Document doc = Document.builder()
                .membre(membre)
                .typeDocument(type)
                .nomFichier(fichier.getOriginalFilename())
                .cheminStockage(objectKey)
                .tailleOctets(fichier.getSize())
                .contentType(contentType)
                .build();

        return DocumentResponse.from(documentRepository.save(doc));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listerParMembre(UUID membreId) {
        return documentRepository.findAllByMembreIdOrderByCreatedAtDesc(membreId)
                .stream().map(DocumentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> mesDocuments(String email) {
        List<Membre> membres = membreRepository.findAllByUserEmail(email);
        if (membres.isEmpty()) {
            throw new IllegalArgumentException("Aucun profil membre associé à ce compte");
        }
        return membres.stream()
                .flatMap(m -> documentRepository.findAllByMembreIdOrderByCreatedAtDesc(m.getId()).stream())
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InputStream telechargerFichier(UUID documentId, String email) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable : " + documentId));

        // Vérification d'accès : propriétaire ou gestionnaire (ADMIN/SECRETAIRE)
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        boolean isGestionnaire = requester.getRole() == User.Role.ADMIN
                || requester.getRole() == User.Role.SECRETAIRE;
        if (!isGestionnaire) {
            boolean isOwner = membreRepository.findAllByUserEmail(email).stream()
                    .anyMatch(m -> m.getId().equals(doc.getMembre().getId()));
            if (!isOwner) {
                throw new IllegalArgumentException("Accès refusé à ce document");
            }
        }

        return minioStorage.telecharger(MinioConfig.BUCKET_MEMBRES, doc.getCheminStockage());
    }

    public String getContentType(UUID documentId) {
        return documentRepository.findById(documentId)
                .map(Document::getContentType)
                .orElse("application/octet-stream");
    }

    public String getNomFichier(UUID documentId) {
        return documentRepository.findById(documentId)
                .map(Document::getNomFichier)
                .orElse("document");
    }

    @Transactional
    public void supprimer(UUID documentId, String email) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable : " + documentId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        boolean isGestionnaire = user.getRole() == User.Role.ADMIN
                || user.getRole() == User.Role.SECRETAIRE;
        if (!isGestionnaire) {
            boolean isOwner = membreRepository.findAllByUserEmail(email).stream()
                    .anyMatch(m -> m.getId().equals(doc.getMembre().getId()));
            if (!isOwner) {
                throw new IllegalArgumentException("Vous ne pouvez supprimer que vos propres documents");
            }
        }

        minioStorage.supprimer(MinioConfig.BUCKET_MEMBRES, doc.getCheminStockage());
        documentRepository.delete(doc);
    }
}
