package com.tontinepro.tontinepro_backend.api.membre;

import com.tontinepro.tontinepro_backend.api.membre.dto.SuiviMoisResponse;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.MouvementEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.pret.EcheancePretRepository;
import com.tontinepro.tontinepro_backend.domain.pret.PretRepository;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SuiviService {

    private final MembreRepository membreRepository;
    private final CotisationRepository cotisationRepository;
    private final ContributionFondsAideRepository contributionRepository;
    private final SanctionRepository sanctionRepository;
    private final PretRepository pretRepository;
    private final EcheancePretRepository echeancePretRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final MouvementEpargneRepository mouvementEpargneRepository;

    @Transactional(readOnly = true)
    public List<SuiviMoisResponse> getSuiviAnnuel(String email, int annee) {
        List<Membre> membres = membreRepository.findAllByUserEmail(email);

        Map<Integer, List<SuiviMoisResponse.CotisationItem>>      cotisationsMap    = new TreeMap<>();
        Map<Integer, List<SuiviMoisResponse.ContributionItem>>     contributionsMap  = new TreeMap<>();
        Map<Integer, List<SuiviMoisResponse.SanctionItem>>         sanctionsMap      = new TreeMap<>();
        Map<Integer, List<SuiviMoisResponse.EcheanceItem>>         echeancesMap      = new TreeMap<>();
        Map<Integer, List<SuiviMoisResponse.MouvementEpargneItem>> mouvementsMap     = new TreeMap<>();

        for (Membre membre : membres) {
            String tontineNom = membre.getTontine().getNom();
            UUID membreId = membre.getId();

            // Cotisations (filtrées par année)
            cotisationRepository.findAllByMembreId(membreId).stream()
                    .filter(c -> c.getAnnee() == (short) annee)
                    .forEach(c -> cotisationsMap
                            .computeIfAbsent((int) c.getMois(), k -> new ArrayList<>())
                            .add(new SuiviMoisResponse.CotisationItem(
                                    c.getId(), tontineNom, c.getMontant(),
                                    c.getStatut().name(), c.getDatePaiement())));

            // Contributions fonds d'aide MENSUEL (filtrées par année)
            contributionRepository.findAllByMembreId(membreId).stream()
                    .filter(c -> c.getAnnee() != null && c.getAnnee() == (short) annee
                                 && c.getMois() != null)
                    .forEach(c -> contributionsMap
                            .computeIfAbsent((int) c.getMois(), k -> new ArrayList<>())
                            .add(new SuiviMoisResponse.ContributionItem(
                                    c.getId(), tontineNom, c.getMontant(),
                                    c.getStatut().name(), c.getDatePaiement())));

            // Sanctions (filtrées par année de createdAt)
            sanctionRepository.findAllByMembreId(membreId).stream()
                    .filter(s -> s.getCreatedAt().getYear() == annee)
                    .forEach(s -> sanctionsMap
                            .computeIfAbsent(s.getCreatedAt().getMonthValue(), k -> new ArrayList<>())
                            .add(new SuiviMoisResponse.SanctionItem(
                                    s.getId(), tontineNom, s.getTypeSanction().name(),
                                    s.getMontant(), s.getMotif(), s.isPayee(), s.getCreatedAt())));

            // Échéances de prêt (filtrées par dateEcheance dans l'année)
            pretRepository.findAllByMembreIdOrderByCreatedAtDesc(membreId).forEach(pret ->
                    echeancePretRepository.findAllByPretIdOrderByNumeroEcheanceAsc(pret.getId()).stream()
                            .filter(e -> e.getDateEcheance().getYear() == annee)
                            .forEach(e -> echeancesMap
                                    .computeIfAbsent(e.getDateEcheance().getMonthValue(), k -> new ArrayList<>())
                                    .add(new SuiviMoisResponse.EcheanceItem(
                                            e.getId(), pret.getId(), tontineNom,
                                            e.getNumeroEcheance(), e.getMontantTotal(),
                                            e.getStatut().name(), e.getDateEcheance(), e.getDatePaiement()))));

            // Mouvements épargne (filtrés par année de createdAt)
            compteEpargneRepository.findByMembreId(membreId).ifPresent(compte ->
                    mouvementEpargneRepository.findAllByCompteIdOrderByCreatedAtDesc(compte.getId()).stream()
                            .filter(m -> m.getCreatedAt().getYear() == annee)
                            .forEach(m -> mouvementsMap
                                    .computeIfAbsent(m.getCreatedAt().getMonthValue(), k -> new ArrayList<>())
                                    .add(new SuiviMoisResponse.MouvementEpargneItem(
                                            m.getId(), tontineNom, m.getTypeMouvement().name(),
                                            m.getMontant(), m.getSoldeApres(), m.getCreatedAt()))));
        }

        // Réunir tous les mois ayant des données
        Set<Integer> moisAvecDonnees = new TreeSet<>();
        moisAvecDonnees.addAll(cotisationsMap.keySet());
        moisAvecDonnees.addAll(contributionsMap.keySet());
        moisAvecDonnees.addAll(sanctionsMap.keySet());
        moisAvecDonnees.addAll(echeancesMap.keySet());
        moisAvecDonnees.addAll(mouvementsMap.keySet());

        return moisAvecDonnees.stream()
                .sorted(Comparator.reverseOrder())
                .map(mois -> new SuiviMoisResponse(
                        mois, annee,
                        cotisationsMap.getOrDefault(mois, List.of()),
                        contributionsMap.getOrDefault(mois, List.of()),
                        sanctionsMap.getOrDefault(mois, List.of()),
                        echeancesMap.getOrDefault(mois, List.of()),
                        mouvementsMap.getOrDefault(mois, List.of())))
                .toList();
    }
}
