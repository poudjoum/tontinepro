package com.tontinepro.tontinepro_backend.domain.aide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArriereMembreRepository extends JpaRepository<ArriereMembre, UUID> {

    List<ArriereMembre> findAllByTontineIdOrderByDateConstatAscCreatedAtAsc(UUID tontineId);

    List<ArriereMembre> findAllByMembreId(UUID membreId);
}
