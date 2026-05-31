package com.tontinepro.tontinepro_backend.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandeReinitMdpRepository extends JpaRepository<DemandeReinitMdp, UUID> {

    List<DemandeReinitMdp> findAllByOrderByCreatedAtDesc();

    List<DemandeReinitMdp> findAllByStatutOrderByCreatedAtDesc(DemandeReinitMdp.Statut statut);
}
