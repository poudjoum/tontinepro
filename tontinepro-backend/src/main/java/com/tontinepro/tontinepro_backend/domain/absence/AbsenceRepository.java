package com.tontinepro.tontinepro_backend.domain.absence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AbsenceRepository extends JpaRepository<Absence, UUID> {

    List<Absence> findAllByTontineId(UUID tontineId);

    List<Absence> findAllByMembreId(UUID membreId);

    Optional<Absence> findByMembreIdAndDateReunion(UUID membreId, LocalDate dateReunion);

    boolean existsByMembreIdAndDateReunion(UUID membreId, LocalDate dateReunion);
}
