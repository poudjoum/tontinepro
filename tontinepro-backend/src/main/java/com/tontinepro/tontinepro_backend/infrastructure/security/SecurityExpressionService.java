package com.tontinepro.tontinepro_backend.infrastructure.security;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
