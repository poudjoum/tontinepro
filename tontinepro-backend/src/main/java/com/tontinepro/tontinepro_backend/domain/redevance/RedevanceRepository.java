package com.tontinepro.tontinepro_backend.domain.redevance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RedevanceRepository extends JpaRepository<Redevance, UUID> {

    Optional<Redevance> findByTontineId(UUID tontineId);
}
