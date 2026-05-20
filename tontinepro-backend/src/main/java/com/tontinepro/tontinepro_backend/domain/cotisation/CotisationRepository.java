package com.tontinepro.tontinepro_backend.domain.cotisation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CotisationRepository extends JpaRepository<Cotisation, UUID> {

    List<Cotisation> findAllByMembreId(UUID membreId);

    List<Cotisation> findAllByTontineId(UUID tontineId);

    List<Cotisation> findAllByTontineIdAndMoisAndAnnee(UUID tontineId, short mois, short annee);

    List<Cotisation> findAllByStatut(Cotisation.Statut statut);

    boolean existsByMembreIdAndMoisAndAnnee(UUID membreId, short mois, short annee);

    List<Cotisation> findAllByTontineIdAndAnnee(UUID tontineId, short annee);

    List<Cotisation> findAllByMembreIdAndAnneeAndStatut(UUID membreId, short annee, Cotisation.Statut statut);
}
