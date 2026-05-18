package com.tontinepro.tontinepro_backend.domain.aide;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContributionFondsAideRepository extends JpaRepository<ContributionFondsAide, UUID> {

    List<ContributionFondsAide> findAllByMembreId(UUID membreId);

    List<ContributionFondsAide> findAllByFondsAideId(UUID fondsAideId);

    List<ContributionFondsAide> findAllByFondsAideIdAndStatut(UUID fondsAideId, ContributionFondsAide.Statut statut);

    boolean existsByFondsAideIdAndMembreIdAndMoisAndAnnee(UUID fondsAideId, UUID membreId, short mois, short annee);
}
