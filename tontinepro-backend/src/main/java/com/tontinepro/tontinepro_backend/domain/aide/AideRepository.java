package com.tontinepro.tontinepro_backend.domain.aide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AideRepository extends JpaRepository<Aide, UUID> {

    List<Aide> findAllByMembreId(UUID membreId);

    List<Aide> findAllByStatut(Aide.Statut statut);

    List<Aide> findAllByMembreTontineId(UUID tontineId);

    List<Aide> findAllByMembreTontineIdAndCreatedAtBetween(UUID tontineId,
            java.time.OffsetDateTime debut, java.time.OffsetDateTime fin);
}