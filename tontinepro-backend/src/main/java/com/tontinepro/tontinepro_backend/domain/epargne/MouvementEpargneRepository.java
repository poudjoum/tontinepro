package com.tontinepro.tontinepro_backend.domain.epargne;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MouvementEpargneRepository extends JpaRepository<MouvementEpargne, UUID> {

    List<MouvementEpargne> findAllByCompteIdOrderByCreatedAtDesc(UUID compteId);
}
