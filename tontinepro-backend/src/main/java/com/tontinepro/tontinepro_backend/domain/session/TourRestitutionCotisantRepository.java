package com.tontinepro.tontinepro_backend.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TourRestitutionCotisantRepository extends JpaRepository<TourRestitutionCotisant, UUID> {

    List<TourRestitutionCotisant> findAllByTourId(UUID tourId);
}
