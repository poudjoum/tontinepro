package com.tontinepro.tontinepro_backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrdreBeneficiaireRepository extends JpaRepository<OrdreBeneficiaire, UUID> {

    List<OrdreBeneficiaire> findAllBySessionIdOrderByOrdre(UUID sessionId);

    List<OrdreBeneficiaire> findAllByMembreIdAndBeneficieTrueOrderByCreatedAtDesc(UUID membreId);
}
