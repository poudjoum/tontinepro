package com.tontinepro.tontinepro_backend.domain.epargne;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompteEpargneRepository extends JpaRepository<CompteEpargne, UUID> {

    Optional<CompteEpargne> findByMembreId(UUID membreId);

    Optional<CompteEpargne> findByMembreUserEmail(String email);

    Optional<CompteEpargne> findByMembreUserEmailAndMembreTontineId(String email, UUID tontineId);

    List<CompteEpargne> findAllByMembreTontineId(UUID tontineId);
}
