package com.tontinepro.tontinepro_backend.domain.membre;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembreRepository extends JpaRepository<Membre, UUID> {

    /** Retourne tous les profils d'un utilisateur (toutes tontines confondues). */
    List<Membre> findAllByUserEmail(String email);

    /** Retourne le profil dans une tontine précise. */
    Optional<Membre> findByUserEmailAndTontineId(String email, UUID tontineId);

    /**
     * Compatibilité ascendante : retourne le premier profil actif.
     * Utilisé dans les contextes qui n'ont pas encore le tontineId.
     */
    default Optional<Membre> findByUserEmail(String email) {
        List<Membre> tous = findAllByUserEmail(email);
        return tous.stream()
                .filter(m -> m.getStatut() == Membre.Statut.ACTIF)
                .findFirst()
                .or(() -> tous.stream().findFirst());
    }

    List<Membre> findAllByTontineId(UUID tontineId);

    List<Membre> findAllByTontineIdAndStatut(UUID tontineId, Membre.Statut statut);

    List<Membre> findAllByTontineIdAndStatutAndTypeParticipation(
            UUID tontineId, Membre.Statut statut, Membre.TypeParticipation typeParticipation);

    List<Membre> findAllByTontineIdAndTypeParticipation(
            UUID tontineId, Membre.TypeParticipation typeParticipation);

    /** Vérifie qu'un utilisateur n'est pas déjà membre de cette tontine précise. */
    boolean existsByUserIdAndTontineId(UUID userId, UUID tontineId);

    boolean existsByUserId(UUID userId);

    boolean existsByMatricule(String matricule);
}
