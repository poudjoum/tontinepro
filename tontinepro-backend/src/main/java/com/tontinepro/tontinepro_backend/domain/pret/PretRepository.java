package com.tontinepro.tontinepro_backend.domain.pret;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PretRepository extends JpaRepository<Pret, UUID> {

    List<Pret> findAllByMembreIdOrderByCreatedAtDesc(UUID membreId);

    List<Pret> findAllByStatut(Pret.Statut statut);

    List<Pret> findAllByMembreTontineId(UUID tontineId);
}
