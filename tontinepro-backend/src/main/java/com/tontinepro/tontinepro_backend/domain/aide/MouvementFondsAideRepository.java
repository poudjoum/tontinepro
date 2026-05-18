package com.tontinepro.tontinepro_backend.domain.aide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MouvementFondsAideRepository extends JpaRepository<MouvementFondsAide, UUID> {

    List<MouvementFondsAide> findAllByFondsAideIdOrderByCreatedAtDesc(UUID fondsAideId);
}
