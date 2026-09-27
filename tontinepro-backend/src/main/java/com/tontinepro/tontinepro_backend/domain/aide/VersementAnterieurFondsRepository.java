package com.tontinepro.tontinepro_backend.domain.aide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VersementAnterieurFondsRepository extends JpaRepository<VersementAnterieurFonds, UUID> {

    List<VersementAnterieurFonds> findAllByTontineIdAndAnnee(UUID tontineId, short annee);

    Optional<VersementAnterieurFonds> findByMembreIdAndAnnee(UUID membreId, short annee);
}
