package com.tontinepro.tontinepro_backend.domain.demande;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandeAdhesionRepository extends JpaRepository<DemandeAdhesion, UUID> {

    List<DemandeAdhesion> findAllByTontineId(UUID tontineId);

    List<DemandeAdhesion> findAllByStatut(DemandeAdhesion.Statut statut);

    boolean existsByEmailAndTontineId(String email, UUID tontineId);
}
