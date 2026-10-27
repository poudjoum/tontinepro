package com.tontinepro.tontinepro_backend.domain.aide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReglementArriereRepository extends JpaRepository<ReglementArriere, UUID> {

    List<ReglementArriere> findAllByArriereIdOrderByDateReglementAscCreatedAtAsc(UUID arriereId);

    List<ReglementArriere> findAllByArriereTontineId(UUID tontineId);
}
