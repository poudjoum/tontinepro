package com.tontinepro.tontinepro_backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AbandonLotRepository extends JpaRepository<AbandonLot, UUID> {

    List<AbandonLot> findAllBySessionIdOrderByDateAbandon(UUID sessionId);

    boolean existsBySessionIdAndMembreId(UUID sessionId, UUID membreId);
}
