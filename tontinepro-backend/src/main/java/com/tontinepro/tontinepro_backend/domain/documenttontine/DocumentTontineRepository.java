package com.tontinepro.tontinepro_backend.domain.documenttontine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentTontineRepository extends JpaRepository<DocumentTontine, UUID> {
    List<DocumentTontine> findAllByTontineId(UUID tontineId);
}
