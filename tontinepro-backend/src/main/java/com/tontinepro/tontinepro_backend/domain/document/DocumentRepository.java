package com.tontinepro.tontinepro_backend.domain.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findAllByMembreId(UUID membreId);

    List<Document> findAllByMembreIdAndTypeDocument(UUID membreId, Document.TypeDocument typeDocument);
}
