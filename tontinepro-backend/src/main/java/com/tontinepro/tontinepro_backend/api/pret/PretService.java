package com.tontinepro.tontinepro_backend.api.pret;

import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.api.pret.dto.*;
import com.tontinepro.tontinepro_backend.domain.document.Document;
import com.tontinepro.tontinepro_backend.domain.document.DocumentRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.pret.EcheancePret;
import com.tontinepro.tontinepro_backend.domain.pret.EcheancePretRepository;
import com.tontinepro.tontinepro_backend.domain.pret.Pret;
import com.tontinepro.tontinepro_backend.domain.pret.PretRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PretService {

    private final PretRepository pretRepository;
    private final EcheancePretRepository echeancePretRepository;
    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final TontineRepository tontineRepository;
    private final NotificationService notificationService;
    private final DocumentRepository documentRepository;

    // ── Membre ───────────────────────────────────────────────────────────

    @Transactional
    public PretResponse soumettreDemande(String email, DemandePretRequest request) {
        Membre membre = membreRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));

        if (membre.getStatut() != Membre.Statut.ACTIF) {
            throw new IllegalArgumentException("Seuls les membres actifs peuvent demander un prêt");
        }

        // Vérifier les documents obligatoires pour un prêt
        verifierDocumentsPret(membre.getId());

        BigDecimal taux = membre.getTontine().getTauxInteretPret();
        BigDecimal montantTotal = calculerMontantTotal(request.montantPrincipal(), taux, request.dureeMois());

        Pret pret = Pret.builder()
                .membre(membre)
                .montantPrincipal(request.montantPrincipal())
                .tauxInteret(taux)
                .dureeMois(request.dureeMois())
                .montantTotal(montantTotal)
                .build();

        PretResponse response = PretResponse.from(pretRepository.save(pret));

        notificationService.notifierAdmins(
                Notification.Type.PRET_DEMANDE,
                "Nouvelle demande de prêt",
                "Le membre %s %s demande un prêt de %s FCFA sur %s mois."
                        .formatted(membre.getNom(), membre.getPrenom(),
                                request.montantPrincipal(), request.dureeMois()),
                response.id(), "PRET");

        return response;
    }

    @Transactional(readOnly = true)
    public List<PretResponse> getMesPrets(String email) {
        return membreRepository.findByUserEmail(email)
                .map(m -> pretRepository.findAllByMembreIdOrderByCreatedAtDesc(m.getId())
                        .stream().map(PretResponse::from).toList())
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));
    }

    @Transactional(readOnly = true)
    public List<EcheancePretResponse> getEcheances(UUID pretId, String email) {
        Pret pret = loadPret(pretId);
        verifierAccesPret(pret, email);
        return echeancePretRepository.findAllByPretIdOrderByNumeroEcheanceAsc(pretId)
                .stream().map(EcheancePretResponse::from).toList();
    }

    @Transactional
    public EcheancePretResponse rembourserProchaineEcheance(UUID pretId, String email) {
        Pret pret = loadPret(pretId);
        verifierAccesPret(pret, email);

        if (pret.getStatut() != Pret.Statut.EN_COURS) {
            throw new IllegalArgumentException("Seuls les prêts EN_COURS peuvent être remboursés");
        }

        EcheancePret echeance = echeancePretRepository
                .findFirstByPretIdAndStatutOrderByNumeroEcheanceAsc(pretId, EcheancePret.Statut.A_PAYER)
                .orElseThrow(() -> new IllegalArgumentException("Aucune échéance en attente pour ce prêt"));

        echeance.setStatut(EcheancePret.Statut.PAYEE);
        echeance.setDatePaiement(OffsetDateTime.now());
        echeancePretRepository.save(echeance);

        boolean pretSolde = echeancePretRepository.countByPretIdAndStatutNot(pretId, EcheancePret.Statut.PAYEE) == 0;
        if (pretSolde) {
            pret.setStatut(Pret.Statut.SOLDE);
            pretRepository.save(pret);
            notificationService.notifier(pret.getMembre().getUser(),
                    Notification.Type.PRET_SOLDE,
                    "Prêt intégralement remboursé",
                    "Félicitations ! Votre prêt de %s FCFA est entièrement remboursé."
                            .formatted(pret.getMontantPrincipal()),
                    pret.getId(), "PRET");
        } else {
            notificationService.notifier(pret.getMembre().getUser(),
                    Notification.Type.PRET_REMBOURSE,
                    "Remboursement enregistré",
                    "Votre remboursement de l'échéance %d a bien été enregistré."
                            .formatted(echeance.getNumeroEcheance()),
                    pret.getId(), "PRET");
        }

        return EcheancePretResponse.from(echeance);
    }

    // ── Admin ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PretResponse> list(UUID tontineId, Pret.Statut statut) {
        List<Pret> prets;
        if (tontineId != null) {
            prets = pretRepository.findAllByMembreTontineId(tontineId);
        } else if (statut != null) {
            prets = pretRepository.findAllByStatut(statut);
        } else {
            prets = pretRepository.findAll();
        }
        return prets.stream().map(PretResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PretResponse getById(UUID id) {
        return PretResponse.from(loadPret(id));
    }

    @Transactional
    public PretResponse valider(UUID id, String adminEmail) {
        Pret pret = loadPret(id);

        if (pret.getStatut() != Pret.Statut.DEMANDE) {
            throw new IllegalArgumentException("Seuls les prêts à l'état DEMANDE peuvent être validés");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        OffsetDateTime maintenant = OffsetDateTime.now();
        pret.setStatut(Pret.Statut.EN_COURS);
        pret.setValidePar(admin);
        pret.setDateValidation(maintenant);
        pret.setDateDecaissement(maintenant);
        pretRepository.save(pret);

        genererEcheancier(pret, maintenant.toLocalDate());

        notificationService.notifier(pret.getMembre().getUser(),
                Notification.Type.PRET_APPROUVE,
                "Prêt approuvé",
                "Votre demande de prêt de %s FCFA sur %s mois a été approuvée et décaissée."
                        .formatted(pret.getMontantPrincipal(), pret.getDureeMois()),
                pret.getId(), "PRET");

        return PretResponse.from(pret);
    }

    @Transactional
    public PretResponse rejeter(UUID id, String adminEmail, RejeterPretRequest request) {
        Pret pret = loadPret(id);

        if (pret.getStatut() != Pret.Statut.DEMANDE) {
            throw new IllegalArgumentException("Seuls les prêts à l'état DEMANDE peuvent être rejetés");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        pret.setStatut(Pret.Statut.REJETE);
        pret.setMotifRejet(request.motif());
        pret.setValidePar(admin);
        pret.setDateValidation(OffsetDateTime.now());
        PretResponse response = PretResponse.from(pretRepository.save(pret));

        notificationService.notifier(pret.getMembre().getUser(),
                Notification.Type.PRET_REJETE,
                "Demande de prêt rejetée",
                "Votre demande de prêt a été rejetée. Motif : %s.".formatted(request.motif()),
                pret.getId(), "PRET");

        return response;
    }

    @Transactional
    public EcheancePretResponse marquerEcheanceEnRetard(UUID echeanceId) {
        EcheancePret echeance = echeancePretRepository.findById(echeanceId)
                .orElseThrow(() -> new IllegalArgumentException("Échéance introuvable : " + echeanceId));

        if (echeance.getStatut() == EcheancePret.Statut.PAYEE) {
            throw new IllegalArgumentException("Impossible de marquer en retard une échéance déjà payée");
        }

        echeance.setStatut(EcheancePret.Statut.EN_RETARD);
        return EcheancePretResponse.from(echeancePretRepository.save(echeance));
    }

    // ── Simulation ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SimulationPretResponse simuler(BigDecimal montantPrincipal, short dureeMois, UUID tontineId) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + tontineId));

        BigDecimal taux = tontine.getTauxInteretPret();
        return calculerSimulation(montantPrincipal, taux, dureeMois);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Génère l'échéancier d'amortissement français (mensualités constantes).
     * Les dates d'échéance démarrent un mois après la date de décaissement.
     */
    private void genererEcheancier(Pret pret, LocalDate dateDecaissement) {
        int n = pret.getDureeMois();
        BigDecimal tauxMensuel = tauxMensuel(pret.getTauxInteret());
        BigDecimal mensualite = mensualite(pret.getMontantPrincipal(), tauxMensuel, n);

        BigDecimal solde = pret.getMontantPrincipal();
        List<EcheancePret> echeances = new ArrayList<>(n);

        for (int i = 1; i <= n; i++) {
            BigDecimal interet = solde.multiply(tauxMensuel).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital;
            BigDecimal mensualiteI;

            if (i == n) {
                // Dernière échéance : solde restant (absorbe les arrondis)
                capital = solde;
                mensualiteI = capital.add(interet);
            } else {
                capital = mensualite.subtract(interet);
                mensualiteI = mensualite;
            }

            solde = solde.subtract(capital);

            echeances.add(EcheancePret.builder()
                    .pret(pret)
                    .numeroEcheance((short) i)
                    .dateEcheance(dateDecaissement.plusMonths(i))
                    .montantCapital(capital)
                    .montantInteret(interet)
                    .montantTotal(mensualiteI)
                    .build());
        }

        echeancePretRepository.saveAll(echeances);
    }

    private SimulationPretResponse calculerSimulation(BigDecimal principal, BigDecimal taux, int n) {
        BigDecimal tauxMensuel = tauxMensuel(taux);
        BigDecimal mensualite = mensualite(principal, tauxMensuel, n);

        BigDecimal solde = principal;
        List<SimulationPretResponse.LigneAmortissement> lignes = new ArrayList<>(n);

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
            lignes.add(new SimulationPretResponse.LigneAmortissement(i, capital, interet, mensualiteI, solde));
        }

        BigDecimal montantTotal = lignes.stream()
                .map(SimulationPretResponse.LigneAmortissement::mensualite)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SimulationPretResponse(
                principal,
                taux,
                n,
                mensualite,
                montantTotal,
                montantTotal.subtract(principal),
                lignes
        );
    }

    /**
     * Calcule le montant total à rembourser (approximation : mensualité × n).
     * La différence de quelques centimes due aux arrondis est absorbée par la dernière échéance.
     */
    private BigDecimal calculerMontantTotal(BigDecimal principal, BigDecimal taux, int n) {
        BigDecimal tauxMensuel = tauxMensuel(taux);
        return mensualite(principal, tauxMensuel, n)
                .multiply(BigDecimal.valueOf(n))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Taux mensuel décimal à partir du taux annuel en % (ex : 12.00 → 0.01). */
    private BigDecimal tauxMensuel(BigDecimal tauxAnnuel) {
        return tauxAnnuel.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
    }

    /**
     * Mensualité constante (formule d'amortissement français).
     * M = P * r / (1 − (1 + r)^(−n))
     */
    private BigDecimal mensualite(BigDecimal principal, BigDecimal tauxMensuel, int n) {
        if (tauxMensuel.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(tauxMensuel);
        // (1 + r)^n avec précision suffisante
        BigDecimal onePlusRPowN = onePlusR.pow(n, new MathContext(15, RoundingMode.HALF_UP));
        // M = P * r * (1+r)^n / ((1+r)^n − 1)
        return principal
                .multiply(tauxMensuel)
                .multiply(onePlusRPowN)
                .divide(onePlusRPowN.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private Pret loadPret(UUID id) {
        return pretRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prêt introuvable : " + id));
    }

    /** Vérifie qu'un membre ne peut consulter que ses propres prêts (les admins passent null). */
    private void verifierAccesPret(Pret pret, String email) {
        if (email != null && !pret.getMembre().getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("Accès non autorisé à ce prêt");
        }
    }

    /**
     * Vérifie que le membre a fourni les deux documents obligatoires pour une demande de prêt :
     * plan de localisation et lettre de reconnaissance de dette.
     */
    private void verifierDocumentsPret(UUID membreId) {
        boolean hasPlan = !documentRepository
                .findAllByMembreIdAndTypeDocument(membreId, Document.TypeDocument.PLAN_LOCALISATION)
                .isEmpty();
        boolean hasReconnaissance = !documentRepository
                .findAllByMembreIdAndTypeDocument(membreId, Document.TypeDocument.RECONNAISSANCE_DETTE)
                .isEmpty();

        if (!hasPlan && !hasReconnaissance) {
            throw new IllegalArgumentException(
                    "Vous devez fournir votre plan de localisation et votre lettre de reconnaissance de dette " +
                    "avant de soumettre une demande de prêt.");
        }
        if (!hasPlan) {
            throw new IllegalArgumentException(
                    "Vous devez fournir votre plan de localisation avant de soumettre une demande de prêt.");
        }
        if (!hasReconnaissance) {
            throw new IllegalArgumentException(
                    "Vous devez fournir votre lettre de reconnaissance de dette (adressée au Président) " +
                    "avant de soumettre une demande de prêt.");
        }
    }
}
