package com.tontinepro.tontinepro_backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TourRestitutionRepository extends JpaRepository<TourRestitution, UUID> {

    Optional<TourRestitution> findBySessionId(UUID sessionId);
}
