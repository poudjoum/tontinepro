package com.tontinepro.tontinepro_backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionTontineRepository extends JpaRepository<SessionTontine, UUID> {

    List<SessionTontine> findAllByTontineIdOrderByNumeroDesc(UUID tontineId);

    Optional<SessionTontine> findTopByTontineIdOrderByNumeroDesc(UUID tontineId);

    boolean existsByTontineIdAndNumero(UUID tontineId, int numero);

    List<SessionTontine> findAllByStatut(SessionTontine.Statut statut);
}
