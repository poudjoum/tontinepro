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
     * Profil du compte dans la tontine visée.
     *
     * <p>Sans tontine précisée, n'est accepté que si le compte n'a qu'un seul
     * profil. Pour un compte présent dans plusieurs tontines, « le premier
     * profil » était arbitraire : une demande de prêt ou un relevé pouvait porter
     * sur une autre tontine que celle affichée. On refuse plutôt que de deviner.</p>
     */
    default Optional<Membre> profil(String email, UUID tontineId) {
        if (tontineId != null) return findByUserEmailAndTontineId(email, tontineId);
        List<Membre> tous = findAllByUserEmail(email);
        if (tous.size() > 1) {
            throw new IllegalArgumentException(
                    "Vous appartenez à plusieurs tontines : précisez la tontine concernée.");
        }
        return tous.stream().findFirst();
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

    /** Génère un matricule « MBR-XXXXXXXX » garanti unique en base. */
    default String genererMatriculeUnique() {
        String matricule;
        do {
            matricule = "MBR-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8).toUpperCase();
        } while (existsByMatricule(matricule));
        return matricule;
    }
}
