package com.tontinepro.tontinepro_backend.api.historique;

import com.tontinepro.tontinepro_backend.api.historique.dto.ImporterHistoriqueRequest;
import com.tontinepro.tontinepro_backend.api.historique.dto.ImporterHistoriqueResponse;
import com.tontinepro.tontinepro_backend.domain.absence.Absence;
import com.tontinepro.tontinepro_backend.domain.absence.AbsenceRepository;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.MouvementEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.MouvementEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.pret.EcheancePret;
import com.tontinepro.tontinepro_backend.domain.pret.EcheancePretRepository;
import com.tontinepro.tontinepro_backend.domain.pret.Pret;
import com.tontinepro.tontinepro_backend.domain.pret.PretRepository;
import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.sanction.SanctionRepository;
import com.tontinepro.tontinepro_backend.domain.session.OrdreBeneficiaire;
import com.tontinepro.tontinepro_backend.domain.session.OrdreBeneficiaireRepository;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontine;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontineRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HistoriqueImportService {

    private final SessionTontineRepository     sessionRepo;
    private final OrdreBeneficiaireRepository  ordreBenefRepo;
    private final MembreRepository             membreRepo;
    private final CotisationRepository         cotisationRepo;
    private final AbsenceRepository            absenceRepo;
    private final SanctionRepository           sanctionRepo;
    private final CompteEpargneRepository      compteEpargneRepo;
    private final MouvementEpargneRepository   mouvementEpargneRepo;
    private final FondsAideRepository          fondsAideRepo;
    private final PretRepository               pretRepo;
    private final EcheancePretRepository       echeancePretRepo;
    private final UserRepository               userRepo;

    @Transactional
    public ImporterHistoriqueResponse importer(UUID sessionId,
                                               ImporterHistoriqueRequest req,
                                               String emailSecretaire) {

        SessionTontine session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session introuvable : " + sessionId));
        Tontine tontine = session.getTontine();

        User secretaire = userRepo.findByEmail(emailSecretaire)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        List<String> avertissements = new ArrayList<>();
        Map<String, Membre> membreParTel = buildMembreMap(tontine.getId(), avertissements);

        int benefImportes = 0, cotImportees = 0, absImportees = 0,
            sanctImportees = 0, epargnesInit = 0, pretsImportes = 0;

        // ── 1. Fond antérieur ────────────────────────────────────────────────
        if (req.fondAnterieurSolde() != null) {
            FondsAide fonds = fondsAideRepo.findByTontineId(tontine.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Fonds d'aide introuvable"));
            fonds.setSolde(req.fondAnterieurSolde());
            fondsAideRepo.save(fonds);
        }

        // ── 2. Épargne initiale (capital fond antérieur / membre) ────────────
        if (req.partEpargneParMembre() != null
                && req.partEpargneParMembre().compareTo(BigDecimal.ZERO) > 0) {

            for (Membre membre : membreParTel.values()) {
                CompteEpargne compte = compteEpargneRepo.findByMembreId(membre.getId())
                        .orElse(null);
                if (compte == null) { continue; }

                BigDecimal ancienSolde = compte.getSolde();
                compte.setSolde(ancienSolde.add(req.partEpargneParMembre()));
                compteEpargneRepo.save(compte);

                mouvementEpargneRepo.save(MouvementEpargne.builder()
                        .compte(compte)
                        .typeMouvement(MouvementEpargne.TypeMouvement.DEPOT)
                        .montant(req.partEpargneParMembre())
                        .soldeApres(compte.getSolde())
                        .reference("IMPORT_HISTORIQUE_CAPITAL")
                        .build());
                epargnesInit++;
            }
        }

        // ── 3. Tours passés (bénéficiaires) ─────────────────────────────────
        List<OrdreBeneficiaire> ordres = ordreBenefRepo.findAllBySessionIdOrderByOrdre(sessionId);

        for (var tour : safe(req.toursPasses())) {
            Membre membre = resolve(tour.telephone(), membreParTel, avertissements,
                    "Tour " + tour.date());
            if (membre == null) continue;

            OrdreBeneficiaire ob = ordres.stream()
                    .filter(o -> o.getMembre().getId().equals(membre.getId()))
                    .findFirst().orElse(null);

            if (ob == null) {
                avertissements.add("Aucun ordre de bénéfice pour " + tour.telephone()
                        + " dans cette session");
                continue;
            }

            ob.setBeneficie(true);
            ob.setDateBenefice(parseDate(tour.date()));
            ob.setMontantRecu(tour.montantRecu());
            ordreBenefRepo.save(ob);
            benefImportes++;
        }

        // ── 4. Cotisations passées ───────────────────────────────────────────
        for (var cot : safe(req.cotisations())) {
            Membre membre = resolve(cot.telephone(), membreParTel, avertissements,
                    "Cotisation " + cot.mois() + "/" + cot.annee());
            if (membre == null) continue;

            if (!cotisationRepo.existsByMembreIdAndMoisAndAnnee(
                    membre.getId(), (short) cot.mois(), (short) cot.annee())) {

                cotisationRepo.save(Cotisation.builder()
                        .membre(membre)
                        .tontine(tontine)
                        .mois((short) cot.mois())
                        .annee((short) cot.annee())
                        .montant(nvl(cot.montantCotisation()))
                        .montantFondAide(nvl(cot.montantFondAide()))
                        .statut(mapStatutCotisation(cot.cotisationStatut()))
                        .build());
                cotImportees++;
            }
        }

        // ── 5. Absences et sanctions ─────────────────────────────────────────
        for (var abs : safe(req.absencesSanctions())) {
            Membre membre = resolve(abs.telephone(), membreParTel, avertissements,
                    "Absence " + abs.dateReunion());
            if (membre == null) continue;

            LocalDate dateReunion = parseDate(abs.dateReunion());

            if (!absenceRepo.existsByMembreIdAndDateReunion(membre.getId(), dateReunion)) {
                Absence absence = absenceRepo.save(Absence.builder()
                        .membre(membre)
                        .tontine(tontine)
                        .dateReunion(dateReunion)
                        .justifiee(abs.justifiee())
                        .enregistreePar(secretaire)
                        .build());
                absImportees++;

                // Sanction associée
                if (!abs.justifiee()
                        && abs.sanctionMontant() != null
                        && abs.sanctionMontant().compareTo(BigDecimal.ZERO) > 0) {

                    Sanction sanction = sanctionRepo.save(Sanction.builder()
                            .membre(membre)
                            .tontine(tontine)
                            .typeSanction(Sanction.TypeSanction.ABSENCE_REUNION)
                            .montant(abs.sanctionMontant())
                            .payee(abs.sanctionPayee())
                            .referenceId(absence.getId())
                            .build());

                    if (abs.sanctionPayee()) {
                        sanction.setPayee(true);
                        sanctionRepo.save(sanction);
                    }
                    sanctImportees++;
                }
            }
        }

        // ── 6. Épargne complémentaire ────────────────────────────────────────
        for (var ep : safe(req.epargnesComplementaires())) {
            Membre membre = resolve(ep.telephone(), membreParTel, avertissements,
                    "Épargne complémentaire");
            if (membre == null) continue;

            CompteEpargne compte = compteEpargneRepo.findByMembreId(membre.getId())
                    .orElse(null);
            if (compte == null) continue;

            compte.setSolde(compte.getSolde().add(ep.montant()));
            compteEpargneRepo.save(compte);

            mouvementEpargneRepo.save(MouvementEpargne.builder()
                    .compte(compte)
                    .typeMouvement(MouvementEpargne.TypeMouvement.DEPOT)
                    .montant(ep.montant())
                    .soldeApres(compte.getSolde())
                    .reference("IMPORT_HISTORIQUE_COMPLEMENT")
                    .build());
        }

        // ── 7. Prêts en cours ────────────────────────────────────────────────
        for (var p : safe(req.prets())) {
            Membre membre = resolve(p.telephone(), membreParTel, avertissements,
                    "Prêt " + p.datePret());
            if (membre == null) continue;

            BigDecimal taux = nvl(tontine.getTauxInteretPret());
            BigDecimal tauxMensuel = taux.divide(BigDecimal.valueOf(1200), 10,
                    RoundingMode.HALF_UP);
            BigDecimal mensualite = mensualite(p.montant(), tauxMensuel, p.dureeMois());
            BigDecimal montantTotal = mensualite
                    .multiply(BigDecimal.valueOf(p.dureeMois()))
                    .setScale(2, RoundingMode.HALF_UP);

            OffsetDateTime dateDecaissement = parseDate(p.datePret()).atStartOfDay()
                    .atOffset(java.time.ZoneOffset.UTC);

            Pret pret = pretRepo.save(Pret.builder()
                    .membre(membre)
                    .montantPrincipal(p.montant())
                    .tauxInteret(taux)
                    .dureeMois((short) p.dureeMois())
                    .montantTotal(montantTotal)
                    .statut(Pret.Statut.EN_COURS)
                    .validePar(secretaire)
                    .dateValidation(dateDecaissement)
                    .dateDecaissement(dateDecaissement)
                    .build());

            genererEcheancierAvecRemboursements(pret, parseDate(p.datePret()),
                    nvl(p.montantRembourse()), tauxMensuel, mensualite);
            pretsImportes++;
        }

        return new ImporterHistoriqueResponse(
                benefImportes, cotImportees, absImportees,
                sanctImportees, epargnesInit, pretsImportes,
                avertissements);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Membre> buildMembreMap(UUID tontineId, List<String> avertissements) {
        Map<String, Membre> map = new HashMap<>();
        membreRepo.findAllByTontineId(tontineId).forEach(m -> {
            String tel = m.getUser().getTelephone();
            if (tel != null) {
                map.put(normaliserTel(tel), m);
            }
        });
        return map;
    }

    private Membre resolve(String telephone, Map<String, Membre> map,
                           List<String> avertissements, String contexte) {
        if (telephone == null || telephone.isBlank()) {
            avertissements.add(contexte + " : téléphone manquant");
            return null;
        }
        Membre m = map.get(normaliserTel(telephone));
        if (m == null) {
            avertissements.add(contexte + " : membre introuvable pour " + telephone);
        }
        return m;
    }

    private String normaliserTel(String tel) {
        return tel.replaceAll("[^0-9]", "");
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private Cotisation.Statut mapStatutCotisation(String s) {
        if (s == null) return Cotisation.Statut.EN_ATTENTE;
        return switch (s.toUpperCase()) {
            case "PAYEE" -> Cotisation.Statut.PAYEE;
            case "EN_RETARD", "IMPAYEE" -> Cotisation.Statut.EN_RETARD;
            default -> Cotisation.Statut.EN_ATTENTE;
        };
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private <T> List<T> safe(List<T> list) {
        return list != null ? list : List.of();
    }

    private void genererEcheancierAvecRemboursements(Pret pret, LocalDate dateDecaissement,
                                                     BigDecimal montantRembourse,
                                                     BigDecimal tauxMensuel,
                                                     BigDecimal mensualite) {
        int n = pret.getDureeMois();
        BigDecimal solde = pret.getMontantPrincipal();
        BigDecimal restantARembourser = montantRembourse;
        List<EcheancePret> echeances = new ArrayList<>(n);

        for (int i = 1; i <= n; i++) {
            BigDecimal interet = solde.multiply(tauxMensuel).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital;
            BigDecimal mensualiteI;

            if (i == n) {
                capital = solde;
                mensualiteI = capital.add(interet);
            } else {
                capital = mensualite.subtract(interet);
                mensualiteI = mensualite;
            }
            solde = solde.subtract(capital);

            EcheancePret.Statut statut = EcheancePret.Statut.A_PAYER;
            OffsetDateTime datePaiement = null;

            if (restantARembourser.compareTo(mensualiteI) >= 0) {
                statut = EcheancePret.Statut.PAYEE;
                datePaiement = dateDecaissement.plusMonths(i).atStartOfDay()
                        .atOffset(java.time.ZoneOffset.UTC);
                restantARembourser = restantARembourser.subtract(mensualiteI);
            }

            echeances.add(EcheancePret.builder()
                    .pret(pret)
                    .numeroEcheance((short) i)
                    .dateEcheance(dateDecaissement.plusMonths(i))
                    .montantCapital(capital)
                    .montantInteret(interet)
                    .montantTotal(mensualiteI)
                    .statut(statut)
                    .datePaiement(datePaiement)
                    .build());
        }
        echeancePretRepo.saveAll(echeances);
    }

    private BigDecimal mensualite(BigDecimal principal, BigDecimal tauxMensuel, int n) {
        if (tauxMensuel.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(tauxMensuel);
        BigDecimal onePlusRPowN = onePlusR.pow(n, new MathContext(15, RoundingMode.HALF_UP));
        return principal
                .multiply(tauxMensuel)
                .multiply(onePlusRPowN)
                .divide(onePlusRPowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }
}