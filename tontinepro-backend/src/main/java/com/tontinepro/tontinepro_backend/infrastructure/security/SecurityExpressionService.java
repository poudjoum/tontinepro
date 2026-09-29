package com.tontinepro.tontinepro_backend.infrastructure.security;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service("sec")
@RequiredArgsConstructor
public class SecurityExpressionService {

    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final TontineDeResolver tontineDe;

    /**
     * Vrai si l'utilisateur administre <em>cette</em> tontine : SUPER_ADMIN, ou
     * Président / Secrétaire actif de la tontine.
     *
     * <p>Le rôle applicatif (ADMIN, SECRETAIRE) est global au compte : il découle
     * de la fonction la plus élevée détenue, toutes tontines confondues. Il ne
     * suffit donc pas — le secrétaire de la tontine A serait gestionnaire de la
     * tontine B dont il n'est que membre, voire étranger. Toute action de gestion
     * doit passer par ce contrôle avec la tontine visée.</p>
     *
     * <p>Une tontine inconnue ({@code null}, ressource introuvable) est refusée,
     * sauf au SUPER_ADMIN qui recevra ensuite le 404 du service.</p>
     */
    public boolean gere(String email, UUID tontineId) {
        if (estSuperAdmin(email)) return true;
        return fonctionDans(email, tontineId)
                .map(f -> f == Membre.Fonction.PRESIDENT || f == Membre.Fonction.SECRETAIRE)
                .orElse(false);
    }

    /** Vrai si l'utilisateur est SUPER_ADMIN ou Président actif de cette tontine. */
    public boolean preside(String email, UUID tontineId) {
        if (estSuperAdmin(email)) return true;
        return fonctionDans(email, tontineId)
                .map(f -> f == Membre.Fonction.PRESIDENT)
                .orElse(false);
    }

    /**
     * Membre actif de cette tontine, ou {@link #gere gestionnaire} : lectures
     * réservées aux participants (sessions, échéancier, rapports, barème).
     */
    public boolean membreOuGere(String email, UUID tontineId) {
        return gere(email, tontineId) || fonctionDans(email, tontineId).isPresent();
    }

    /**
     * Mouvements d'argent au nom d'un membre (remboursement de prêt, dépôt ou
     * retrait d'épargne) : gestionnaire, ou membre du bureau qui encaisse
     * ({@link #peutEncaisser}). Jamais le membre lui-même — il se déclarerait
     * remboursé ou créditerait son épargne sans rien verser.
     */
    public boolean bureauEncaisse(String email, UUID tontineId) {
        return gere(email, tontineId) || peutEncaisser(email, tontineId);
    }

    /** {@link #gere} ou Censeur actif de cette tontine (absences, sanctions). */
    public boolean gereOuCenseur(String email, UUID tontineId) {
        return gere(email, tontineId)
                || fonctionDans(email, tontineId).map(f -> f == Membre.Fonction.CENSEUR).orElse(false);
    }

    /**
     * Contrôle d'une liste filtrable par tontine et/ou par membre : chaque filtre
     * fourni doit viser une tontine administrée. Sans aucun filtre, la liste
     * couvrirait toute la plateforme — réservé au SUPER_ADMIN.
     */
    public boolean gereFiltre(String email, UUID tontineId, UUID membreId) {
        if (tontineId == null && membreId == null) return estSuperAdmin(email);
        return (tontineId == null || gere(email, tontineId))
                && (membreId == null || gere(email, tontineDe.membre(membreId)));
    }

    private boolean estSuperAdmin(String email) {
        return userRepository.findByEmail(email)
                .map(u -> u.getRole() == User.Role.SUPER_ADMIN)
                .orElse(false);
    }

    /** Fonction de l'utilisateur dans la tontine, s'il y est membre actif. */
    private Optional<Membre.Fonction> fonctionDans(String email, UUID tontineId) {
        if (tontineId == null) return Optional.empty();
        return membreRepository.findByUserEmailAndTontineId(email, tontineId)
                .filter(m -> m.getStatut() == Membre.Statut.ACTIF)
                .map(Membre::getFonction);
    }



    /**
     * Vrai si l'utilisateur peut encaisser dans cette tontine : parts d'aide,
     * contributions au fonds d'aide, amendes.
     *
     * <p>L'encaissement revient au Trésorier de la tontine. Le Président y garde
     * accès en tant que responsable du bureau. Le Secrétaire n'y a droit que si
     * la tontine n'a désigné aucun trésorier — sans ce filet, une tontine sans
     * trésorier ne pourrait plus rien encaisser.</p>
     */
    public boolean peutEncaisser(String email, UUID tontineId) {
        Membre membre = membreRepository.findByUserEmailAndTontineId(email, tontineId)
                .orElse(null);
        if (membre == null || membre.getStatut() != Membre.Statut.ACTIF) {
            return false;
        }
        if (membre.getFonction() == Membre.Fonction.TRESORIER
                || membre.getFonction() == Membre.Fonction.PRESIDENT) {
            return true;
        }
        return membre.getFonction() == Membre.Fonction.SECRETAIRE
                && !tontineADesigneUnTresorier(tontineId);
    }

    private boolean tontineADesigneUnTresorier(UUID tontineId) {
        return membreRepository.findAllByTontineIdAndStatut(tontineId, Membre.Statut.ACTIF)
                .stream()
                .anyMatch(m -> m.getFonction() == Membre.Fonction.TRESORIER);
    }
}
