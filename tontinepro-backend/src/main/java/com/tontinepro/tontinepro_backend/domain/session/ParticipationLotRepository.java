package com.tontinepro.tontinepro_backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipationLotRepository extends JpaRepository<ParticipationLot, UUID> {

    List<ParticipationLot> findAllBySessionId(UUID sessionId);

    Optional<ParticipationLot> findBySessionIdAndMembreId(UUID sessionId, UUID membreId);

    boolean existsBySessionIdAndMembreId(UUID sessionId, UUID membreId);
}
