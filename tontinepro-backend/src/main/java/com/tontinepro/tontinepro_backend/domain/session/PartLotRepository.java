package com.tontinepro.tontinepro_backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartLotRepository extends JpaRepository<PartLot, UUID> {

    List<PartLot> findAllByOrdreBeneficiaireId(UUID ordreBeneficiaireId);

    List<PartLot> findAllByOrdreBeneficiaireSessionId(UUID sessionId);

    /** Parts déjà versées à un membre dont la retenue porte sur l'année donnée. */
    List<PartLot> findAllByMembreIdAndVerseTrueAndAnneeRetenue(UUID membreId, Short anneeRetenue);
}
