package com.tontinepro.tontinepro_backend.infrastructure.security;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("sec")
@RequiredArgsConstructor
public class SecurityExpressionService {

    private final MembreRepository membreRepository;

    /** Vrai si l'utilisateur est Censeur dans son tontine. */
    public boolean isCenseur(String email) {
        return membreRepository.findByUserEmail(email)
                .map(m -> m.getFonction() == Membre.Fonction.CENSEUR)
                .orElse(false);
    }

    /** Vrai si l'utilisateur est Censeur, Secrétaire ou Président. */
    public boolean isBureauOuAdmin(String email) {
        return membreRepository.findByUserEmail(email)
                .map(m -> m.getFonction() != Membre.Fonction.MEMBRE_ORDINAIRE)
                .orElse(false);
    }

    /** Vrai si l'utilisateur est Trésorier dans au moins une de ses tontines. */
    public boolean isTresorier(String email) {
        return membreRepository.findAllByUserEmail(email).stream()
                .anyMatch(m -> m.getStatut() == Membre.Statut.ACTIF
                        && m.getFonction() == Membre.Fonction.TRESORIER);
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
