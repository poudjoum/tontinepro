package com.tontinepro.tontinepro_backend.domain.membre;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembreRepository extends JpaRepository<Membre, UUID> {

    Optional<Membre> findByUserEmail(String email);

    List<Membre> findAllByTontineId(UUID tontineId);

    List<Membre> findAllByTontineIdAndStatut(UUID tontineId, Membre.Statut statut);

    boolean existsByUserId(UUID userId);

    boolean existsByMatricule(String matricule);
}
