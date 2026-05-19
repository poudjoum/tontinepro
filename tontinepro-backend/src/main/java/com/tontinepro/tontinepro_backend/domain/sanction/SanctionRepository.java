package com.tontinepro.tontinepro_backend.domain.sanction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SanctionRepository extends JpaRepository<Sanction, UUID> {

    List<Sanction> findAllByTontineId(UUID tontineId);

    List<Sanction> findAllByMembreId(UUID membreId);

    List<Sanction> findAllByTontineIdAndPayee(UUID tontineId, boolean payee);
}
