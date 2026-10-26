package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.domain.aide.VersementAnterieurFonds;
import com.tontinepro.tontinepro_backend.domain.aide.VersementAnterieurFondsRepository;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Retenue du fond de caisse (fonds d'aide) au bénéfice : seul le <b>reste dû</b> de
 * l'année est retenu. Point unique de calcul, partagé par les tours classiques et
 * les parts des tontines à lot.
 */
@Component
@RequiredArgsConstructor
public class RetenueFondCaisse {

    private final CotisationRepository cotisationRepository;
    private final VersementAnterieurFondsRepository versementAnterieurRepository;

    /**
     * Fond de caisse déjà versé par le membre pour l'année : part fonds d'aide des
     * cotisations payées dans l'application + versement antérieur déclaré à la
     * reprise (argent collecté avant l'application).
     */
    public BigDecimal dejaVerse(UUID membreId, short annee) {
        BigDecimal viaCotisations = cotisationRepository
                .findAllByMembreIdAndAnneeAndStatut(membreId, annee, Cotisation.Statut.PAYEE)
                .stream()
                .map(c -> c.getMontantFondAide() != null ? c.getMontantFondAide() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal anterieur = versementAnterieurRepository.findByMembreIdAndAnnee(membreId, annee)
                .map(VersementAnterieurFonds::getMontant)
                .orElse(BigDecimal.ZERO);
        return viaCotisations.add(anterieur);
    }

    /**
     * Reste dû du fond de caisse annuel, retenu sur le pot au bénéfice :
     * obligation annuelle − déjà versé, jamais négatif. Un membre qui a versé
     * son fond mensuellement (ou avant l'application) n'est pas prélevé deux fois.
     */
    public BigDecimal resteDu(Tontine tontine, UUID membreId, short annee) {
        BigDecimal obligation = tontine.getMontantFondAideAnnuelMembre() != null
                ? tontine.getMontantFondAideAnnuelMembre() : BigDecimal.ZERO;
        if (obligation.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal reste = obligation.subtract(dejaVerse(membreId, annee));
        return reste.signum() > 0 ? reste : BigDecimal.ZERO;
    }
}
