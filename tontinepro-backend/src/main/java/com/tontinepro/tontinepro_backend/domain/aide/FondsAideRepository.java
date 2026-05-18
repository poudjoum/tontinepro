package com.tontinepro.tontinepro_backend.domain.aide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FondsAideRepository extends JpaRepository<FondsAide, UUID> {

    Optional<FondsAide> findByTontineId(UUID tontineId);
}
