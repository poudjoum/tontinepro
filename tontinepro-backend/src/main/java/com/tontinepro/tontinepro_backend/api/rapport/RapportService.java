package com.tontinepro.tontinepro_backend.api.rapport;

import com.tontinepro.tontinepro_backend.api.rapport.builder.RapportCotisationsExcelBuilder;
import com.tontinepro.tontinepro_backend.api.rapport.builder.RapportFinancierPdfBuilder;
import com.tontinepro.tontinepro_backend.api.rapport.builder.RapportMensuelPdfBuilder;
import com.tontinepro.tontinepro_backend.domain.aide.AideRepository;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.pret.PretRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RapportService {

    private final TontineRepository         tontineRepository;
    private final CotisationRepository      cotisationRepository;
    private final MembreRepository          membreRepository;
    private final AideRepository            aideRepository;
    private final PretRepository            pretRepository;
    private final CompteEpargneRepository   compteEpargneRepository;
    private final FondsAideRepository       fondsAideRepository;
    private final ContributionFondsAideRepository contributionRepository;

    private final RapportMensuelPdfBuilder       mensuelBuilder;
    private final RapportFinancierPdfBuilder      financierBuilder;
    private final RapportCotisationsExcelBuilder  cotisationsBuilder;

    @Transactional(readOnly = true)
    public byte[] rapportMensuel(UUID tontineId, short mois, short annee) {
        if (mois < 1 || mois > 12) throw new IllegalArgumentException("Mois invalide : " + mois);

        Tontine tontine = loadTontine(tontineId);

        var cotisations = cotisationRepository.findAllByTontineIdAndMoisAndAnnee(tontineId, mois, annee);
        var membres     = membreRepository.findAllByTontineId(tontineId);
        var debut       = OffsetDateTime.of(annee, mois, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var fin         = debut.plusMonths(1);
        var aides       = aideRepository.findAllByMembreTontineIdAndCreatedAtBetween(tontineId, debut, fin);

        return mensuelBuilder.build(tontine, mois, annee, cotisations, membres, aides);
    }

    @Transactional(readOnly = true)
    public byte[] rapportCotisations(UUID tontineId, Short mois, Short annee) {
        Tontine tontine = loadTontine(tontineId);

        List<Cotisation> cotisations;
        if (mois != null && annee != null) {
            cotisations = cotisationRepository.findAllByTontineIdAndMoisAndAnnee(tontineId, mois, annee);
        } else if (annee != null) {
            cotisations = cotisationRepository.findAllByTontineIdAndAnnee(tontineId, annee);
        } else {
            cotisations = cotisationRepository.findAllByTontineId(tontineId);
        }

        return cotisationsBuilder.build(tontine, cotisations);
    }

    @Transactional(readOnly = true)
    public byte[] rapportFinancier(UUID tontineId) {
        Tontine tontine = loadTontine(tontineId);

        var comptes       = compteEpargneRepository.findAllByMembreTontineId(tontineId);
        var tousLesPrets  = pretRepository.findAllByMembreTontineId(tontineId);
        var fondsAide     = fondsAideRepository.findByTontineId(tontineId)
                .orElseGet(() -> FondsAide.builder().tontine(tontine).build());
        var contributions = fondsAide.getId() != null
                ? contributionRepository.findAllByFondsAideIdAndStatut(
                        fondsAide.getId(), ContributionFondsAide.Statut.A_PAYER)
                : List.of();
        short anneeEnCours = (short) LocalDate.now().getYear();
        var cotisationsAnnee = cotisationRepository.findAllByTontineIdAndAnnee(tontineId, anneeEnCours);

        return financierBuilder.build(tontine, comptes, tousLesPrets,
                fondsAide, contributions, cotisationsAnnee);
    }

    private Tontine loadTontine(UUID id) {
        return tontineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + id));
    }
}
