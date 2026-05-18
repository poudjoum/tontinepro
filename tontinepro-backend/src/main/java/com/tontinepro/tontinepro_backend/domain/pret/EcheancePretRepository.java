package com.tontinepro.tontinepro_backend.domain.pret;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EcheancePretRepository extends JpaRepository<EcheancePret, UUID> {

    List<EcheancePret> findAllByPretIdOrderByNumeroEcheanceAsc(UUID pretId);

    Optional<EcheancePret> findFirstByPretIdAndStatutOrderByNumeroEcheanceAsc(UUID pretId, EcheancePret.Statut statut);

    long countByPretIdAndStatutNot(UUID pretId, EcheancePret.Statut statut);
}
