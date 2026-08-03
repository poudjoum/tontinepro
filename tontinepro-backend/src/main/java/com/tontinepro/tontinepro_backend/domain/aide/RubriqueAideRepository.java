package com.tontinepro.tontinepro_backend.domain.aide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RubriqueAideRepository extends JpaRepository<RubriqueAide, UUID> {

    List<RubriqueAide> findAllByTontineIdOrderByLibelleAsc(UUID tontineId);

    List<RubriqueAide> findAllByTontineIdAndActifOrderByLibelleAsc(UUID tontineId, boolean actif);
}
