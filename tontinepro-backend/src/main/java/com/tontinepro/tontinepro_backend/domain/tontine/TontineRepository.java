package com.tontinepro.tontinepro_backend.domain.tontine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TontineRepository extends JpaRepository<Tontine, UUID> {

    List<Tontine> findAllByActifTrue();

    boolean existsByNom(String nom);
}
