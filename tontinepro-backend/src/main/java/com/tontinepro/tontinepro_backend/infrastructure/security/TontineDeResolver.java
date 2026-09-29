package com.tontinepro.tontinepro_backend.infrastructure.security;

import com.tontinepro.tontinepro_backend.domain.aide.AideRepository;
import com.tontinepro.tontinepro_backend.domain.aide.RubriqueAideRepository;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.demande.DemandeAdhesionRepository;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.pret.EcheancePretRepository;
import com.tontinepro.tontinepro_backend.domain.pret.PretRepository;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Retrouve la tontine à laquelle appartient une ressource, pour les contrôles
 * d'accès des actions qui ne reçoivent qu'un identifiant de ressource :
 * {@code @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#id))")}.
 *
 * <p>Renvoie {@code null} si la ressource est introuvable : le contrôle refuse
 * alors l'accès (sauf au SUPER_ADMIN, qui obtient le 404 du service).</p>
 */
@Component("tontineDe")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TontineDeResolver {

    private final MembreRepository membreRepository;
    private final SessionTontineRepository sessionRepository;
    private final CotisationRepository cotisationRepository;
    private final AideRepository aideRepository;
    private final RubriqueAideRepository rubriqueRepository;
    private final PretRepository pretRepository;
    private final EcheancePretRepository echeanceRepository;
    private final DemandeAdhesionRepository demandeRepository;

    public UUID membre(UUID id) {
        if (id == null) return null;
        return membreRepository.findById(id).map(m -> m.getTontine().getId()).orElse(null);
    }

    public UUID session(UUID id) {
        if (id == null) return null;
        return sessionRepository.findById(id).map(s -> s.getTontine().getId()).orElse(null);
    }

    public UUID cotisation(UUID id) {
        if (id == null) return null;
        return cotisationRepository.findById(id).map(c -> c.getTontine().getId()).orElse(null);
    }

    public UUID aide(UUID id) {
        if (id == null) return null;
        return aideRepository.findById(id).map(a -> a.getMembre().getTontine().getId()).orElse(null);
    }

    public UUID rubrique(UUID id) {
        if (id == null) return null;
        return rubriqueRepository.findById(id).map(r -> r.getTontine().getId()).orElse(null);
    }

    public UUID pret(UUID id) {
        if (id == null) return null;
        return pretRepository.findById(id).map(p -> p.getMembre().getTontine().getId()).orElse(null);
    }

    public UUID echeance(UUID id) {
        if (id == null) return null;
        return echeanceRepository.findById(id)
                .map(e -> e.getPret().getMembre().getTontine().getId()).orElse(null);
    }

    public UUID demande(UUID id) {
        if (id == null) return null;
        return demandeRepository.findById(id).map(d -> d.getTontine().getId()).orElse(null);
    }
}
