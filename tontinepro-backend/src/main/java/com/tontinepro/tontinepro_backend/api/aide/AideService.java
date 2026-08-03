package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.AideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.AideSuiviResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.DemandeAideRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.RejeterAideRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.SaisirAidePourMembreRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.SimulationAideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.ValiderAideRequest;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.aide.*;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AideService {

    private final AideRepository aideRepository;
    private final MembreRepository membreRepository;
    private final UserRepository userRepository;
    private final FondsAideRepository fondsAideRepository;
    private final MouvementFondsAideRepository mouvementFondsAideRepository;
    private final ContributionFondsAideRepository contributionFondsAideRepository;
    private final RubriqueAideRepository rubriqueAideRepository;
    private final RubriqueAideService rubriqueAideService;
    private final NotificationService notificationService;

    @Transactional
    public AideResponse soumettreDemande(String email, DemandeAideRequest request) {
        Aide.TypeAide typeAide;
        BigDecimal montant;
        RubriqueAide rubrique = null;
        Membre membre;

        if (request.rubriqueId() != null) {
            // Demande issue du barème : type + montant déduits de la rubrique
            rubrique = rubriqueAideRepository.findById(request.rubriqueId())
                    .orElseThrow(() -> new IllegalArgumentException("Rubrique d'aide introuvable"));
            if (!rubrique.isActif()) {
                throw new IllegalArgumentException("Cette rubrique d'aide n'est pas active");
            }
            membre = membreRepository.findByUserEmailAndTontineId(email, rubrique.getTontine().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vous n'êtes pas membre de cette tontine"));
            typeAide = rubrique.getTypeAide();
            montant  = rubriqueAideService.simuler(rubrique.getId()).total();
        } else {
            // Demande libre : type + montant fournis directement
            if (request.typeAide() == null || request.montantDemande() == null) {
                throw new IllegalArgumentException("Le type et le montant sont obligatoires pour une aide libre");
            }
            membre = membreRepository.findByUserEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));
            typeAide = request.typeAide();
            montant  = request.montantDemande();
        }

        if (membre.getStatut() != Membre.Statut.ACTIF) {
            throw new IllegalArgumentException("Seuls les membres actifs peuvent soumettre une demande d'aide");
        }

        Aide aide = Aide.builder()
                .membre(membre)
                .rubrique(rubrique)
                .typeAide(typeAide)
                .montantDemande(montant)
                .motif(request.motif())
                .justificatifUrl(request.justificatifUrl())
                .build();

        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifierAdmins(
                Notification.Type.AIDE_SOUMISE,
                "Nouvelle demande d'aide",
                "Le membre %s %s a soumis une demande d'aide (%s) de %s FCFA."
                        .formatted(membre.getNom(), membre.getPrenom(),
                                typeAide, montant),
                response.id(), "AIDE");

        return response;
    }

    /**
     * Saisie d'une aide par le bureau (secrétaire/admin) au nom d'un membre.
     * L'aide est créée à l'état PROPOSEE et attend l'accord du membre bénéficiaire.
     */
    @Transactional
    public AideResponse saisirPourMembre(String secretaireEmail, SaisirAidePourMembreRequest request) {
        RubriqueAide rubrique = rubriqueAideRepository.findById(request.rubriqueId())
                .orElseThrow(() -> new IllegalArgumentException("Rubrique d'aide introuvable"));
        if (!rubrique.isActif()) {
            throw new IllegalArgumentException("Cette rubrique d'aide n'est pas active");
        }

        Membre membre = membreRepository.findById(request.membreId())
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable"));
        if (!membre.getTontine().getId().equals(rubrique.getTontine().getId())) {
            throw new IllegalArgumentException("Le membre n'appartient pas à la tontine de cette rubrique");
        }
        if (membre.getStatut() != Membre.Statut.ACTIF) {
            throw new IllegalArgumentException("Seuls les membres actifs peuvent bénéficier d'une aide");
        }

        BigDecimal montant = rubriqueAideService.simuler(rubrique.getId()).total();

        Aide aide = Aide.builder()
                .membre(membre)
                .rubrique(rubrique)
                .typeAide(rubrique.getTypeAide())
                .montantDemande(montant)
                .motif(request.motif())
                .justificatifUrl(request.justificatifUrl())
                .statut(Aide.Statut.PROPOSEE)
                .build();

        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifier(membre.getUser(),
                Notification.Type.AIDE_PROPOSEE,
                "Une aide vous est proposée",
                "Le bureau a saisi une aide « %s » (%s FCFA) à votre nom. Marquez votre accord pour la valider."
                        .formatted(rubrique.getLibelle(), montant),
                aide.getId(), "AIDE");

        return response;
    }

    /** Le membre bénéficiaire accepte une aide PROPOSEE → passe à SOUMISE (prête à activer). */
    @Transactional
    public AideResponse accepterProposition(UUID id, String membreEmail) {
        Aide aide = chargerPropositionDuMembre(id, membreEmail);
        aide.setStatut(Aide.Statut.SOUMISE);
        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifierAdmins(
                Notification.Type.AIDE_SOUMISE,
                "Proposition d'aide acceptée",
                "%s %s a accepté l'aide « %s ». Elle peut être activée."
                        .formatted(aide.getMembre().getNom(), aide.getMembre().getPrenom(),
                                aide.getRubrique() != null ? aide.getRubrique().getLibelle() : ""),
                aide.getId(), "AIDE");

        return response;
    }

    /** Le membre bénéficiaire refuse une aide PROPOSEE → passe à REFUSEE. */
    @Transactional
    public AideResponse refuserProposition(UUID id, String membreEmail) {
        Aide aide = chargerPropositionDuMembre(id, membreEmail);
        aide.setStatut(Aide.Statut.REFUSEE);
        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifierAdmins(
                Notification.Type.AIDE_SOUMISE,
                "Proposition d'aide refusée",
                "%s %s a refusé l'aide « %s »."
                        .formatted(aide.getMembre().getNom(), aide.getMembre().getPrenom(),
                                aide.getRubrique() != null ? aide.getRubrique().getLibelle() : ""),
                aide.getId(), "AIDE");

        return response;
    }

    private Aide chargerPropositionDuMembre(UUID id, String membreEmail) {
        Aide aide = aideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));
        if (aide.getStatut() != Aide.Statut.PROPOSEE) {
            throw new IllegalArgumentException("Seule une aide proposée peut être acceptée ou refusée");
        }
        if (aide.getMembre().getUser() == null
                || !membreEmail.equals(aide.getMembre().getUser().getEmail())) {
            throw new IllegalArgumentException("Cette proposition ne vous concerne pas");
        }
        return aide;
    }

    @Transactional(readOnly = true)
    public AideResponse getById(UUID id) {
        return aideRepository.findById(id)
                .map(AideResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));
    }

    @Transactional(readOnly = true)
    public List<AideResponse> list(UUID membreId, UUID tontineId, Aide.Statut statut) {
        List<Aide> aides;

        if (membreId != null) {
            aides = aideRepository.findAllByMembreId(membreId);
        } else if (tontineId != null) {
            aides = aideRepository.findAllByMembreTontineId(tontineId);
        } else if (statut != null) {
            aides = aideRepository.findAllByStatut(statut);
        } else {
            aides = aideRepository.findAll();
        }

        return aides.stream().map(AideResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AideResponse> getMesDemandes(String email) {
        return getMesDemandes(email, null);
    }

    @Transactional(readOnly = true)
    public List<AideResponse> getMesDemandes(String email, java.util.UUID tontineId) {
        var membre = tontineId != null
                ? membreRepository.findByUserEmailAndTontineId(email, tontineId)
                : membreRepository.findByUserEmail(email);
        return membre
                .map(m -> aideRepository.findAllByMembreId(m.getId())
                        .stream().map(AideResponse::from).toList())
                .orElse(List.of());
    }

    @Transactional
    public AideResponse valider(UUID id, String adminEmail, ValiderAideRequest request) {
        Aide aide = aideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));

        if (aide.getRubrique() != null) {
            throw new IllegalArgumentException("Aide issue du barème : utilisez l'activation (activer)");
        }
        if (aide.getStatut() != Aide.Statut.SOUMISE) {
            throw new IllegalArgumentException("Seules les demandes à l'état SOUMISE peuvent être validées");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        aide.setStatut(Aide.Statut.VALIDEE);
        aide.setMontantAccorde(request.montantAccorde());
        aide.setValidePar(admin);
        aide.setDateValidation(OffsetDateTime.now());

        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifier(aide.getMembre().getUser(),
                Notification.Type.AIDE_VALIDEE,
                "Demande d'aide approuvée",
                "Votre demande d'aide a été approuvée. Montant accordé : %s FCFA."
                        .formatted(request.montantAccorde()),
                aide.getId(), "AIDE");

        return response;
    }

    @Transactional
    public AideResponse rejeter(UUID id, String adminEmail, RejeterAideRequest request) {
        Aide aide = aideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));

        if (aide.getStatut() != Aide.Statut.SOUMISE) {
            throw new IllegalArgumentException("Seules les demandes à l'état SOUMISE peuvent être rejetées");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        aide.setStatut(Aide.Statut.REJETEE);
        aide.setMotifRejet(request.motifRejet());
        aide.setValidePar(admin);
        aide.setDateValidation(OffsetDateTime.now());

        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifier(aide.getMembre().getUser(),
                Notification.Type.AIDE_REJETEE,
                "Demande d'aide rejetée",
                "Votre demande d'aide a été rejetée. Motif : %s.".formatted(request.motifRejet()),
                aide.getId(), "AIDE");

        return response;
    }

    @Transactional
    public AideResponse marquerPayee(UUID id) {
        Aide aide = aideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));

        if (aide.getRubrique() != null) {
            throw new IllegalArgumentException(
                    "Aide issue du barème : utilisez le versement dédié (verser)");
        }

        if (aide.getStatut() != Aide.Statut.VALIDEE) {
            throw new IllegalArgumentException("Seules les demandes à l'état VALIDEE peuvent être marquées payées");
        }

        Tontine tontine = aide.getMembre().getTontine();
        Tontine.ModeContributionAide mode = tontine.getModeContributionAide();

        if (mode != Tontine.ModeContributionAide.AUCUN) {
            FondsAide fonds = fondsAideRepository.findByTontineId(tontine.getId())
                    .orElseThrow(() -> new IllegalStateException("Fonds d'aide introuvable pour la tontine"));

            if (mode == Tontine.ModeContributionAide.MENSUEL
                    && fonds.getSolde().compareTo(aide.getMontantAccorde()) < 0) {
                throw new IllegalArgumentException(
                        "Solde du fonds insuffisant (%s disponible, %s requis)"
                                .formatted(fonds.getSolde(), aide.getMontantAccorde()));
            }

            fonds.setSolde(fonds.getSolde().subtract(aide.getMontantAccorde()));
            fondsAideRepository.save(fonds);

            mouvementFondsAideRepository.save(MouvementFondsAide.builder()
                    .fondsAide(fonds)
                    .typeMouvement(MouvementFondsAide.TypeMouvement.DECAISSEMENT)
                    .montant(aide.getMontantAccorde())
                    .soldeApres(fonds.getSolde())
                    .aide(aide)
                    .description("Décaissement aide %s — %s".formatted(aide.getTypeAide(), aide.getMembre().getMatricule()))
                    .build());

            if (mode == Tontine.ModeContributionAide.A_LA_BENEFICIATION) {
                genererContributionsALaBeneficiation(aide, fonds);
            }
        }

        aide.setStatut(Aide.Statut.PAYEE);
        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifier(aide.getMembre().getUser(),
                Notification.Type.AIDE_PAYEE,
                "Aide versée",
                "Votre aide de %s FCFA a été versée.".formatted(aide.getMontantAccorde()),
                aide.getId(), "AIDE");

        return response;
    }

    /**
     * Suivi d'une aide activée : détail des contributions, avancement de la
     * collecte et solde courant du fonds.
     */
    @Transactional(readOnly = true)
    public AideSuiviResponse getSuivi(UUID id) {
        Aide aide = aideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));

        if (aide.getNbMembresBase() == null) {
            throw new IllegalArgumentException("Cette aide n'a pas encore été activée");
        }

        UUID beneficiaireId = aide.getMembre().getId();
        List<ContributionFondsAide> contributions =
                contributionFondsAideRepository.findAllByAideIdOrderByCreatedAtAsc(id);

        BigDecimal totalAttendu  = BigDecimal.ZERO;
        BigDecimal totalCollecte = BigDecimal.ZERO;
        int nbPayes = 0;
        List<AideSuiviResponse.LigneContribution> lignes = new ArrayList<>(contributions.size());
        for (ContributionFondsAide c : contributions) {
            totalAttendu = totalAttendu.add(safe(c.getMontant()));
            boolean payee = c.getStatut() == ContributionFondsAide.Statut.PAYEE;
            if (payee) {
                totalCollecte = totalCollecte.add(safe(c.getMontant()));
                nbPayes++;
            }
            lignes.add(new AideSuiviResponse.LigneContribution(
                    c.getId(),
                    c.getMembre().getId(),
                    c.getMembre().getNom() + " " + c.getMembre().getPrenom(),
                    c.getMembre().getMatricule(),
                    c.getMontant(),
                    c.getStatut(),
                    c.getDatePaiement(),
                    c.getMembre().getId().equals(beneficiaireId)));
        }

        BigDecimal soldeFonds = fondsAideRepository.findByTontineId(aide.getMembre().getTontine().getId())
                .map(FondsAide::getSolde).orElse(BigDecimal.ZERO);

        return new AideSuiviResponse(
                aide.getId(),
                aide.getRubrique() != null ? aide.getRubrique().getLibelle() : null,
                aide.getMembre().getNom() + " " + aide.getMembre().getPrenom(),
                aide.getMembre().getMatricule(),
                aide.getStatut(),
                aide.isPrefinance(),
                aide.getMontantAccorde(),
                aide.getPartParMembre(),
                aide.getNbMembresBase(),
                totalAttendu,
                totalCollecte,
                nbPayes,
                contributions.size(),
                soldeFonds,
                lignes);
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * Active une aide issue du barème (état SOUMISE) : fige le calcul, génère une
     * contribution par membre actif (bénéficiaire inclus), et si préfinancée, décaisse
     * immédiatement le total (le solde peut passer négatif = avance de trésorerie).
     */
    @Transactional
    public AideResponse activerAide(UUID id, boolean prefinance, String adminEmail) {
        Aide aide = aideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));

        if (aide.getRubrique() == null) {
            throw new IllegalArgumentException("Cette demande n'est pas issue du barème");
        }
        if (aide.getStatut() != Aide.Statut.SOUMISE) {
            throw new IllegalArgumentException("Seules les demandes à l'état SOUMISE peuvent être activées");
        }

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        RubriqueAide rubrique = aide.getRubrique();
        if (prefinance && !rubrique.isPrefinancable()) {
            throw new IllegalArgumentException("Cette rubrique n'est pas préfinançable par la trésorerie");
        }

        Tontine tontine = aide.getMembre().getTontine();
        List<Membre> membresActifs = membreRepository.findAllByTontineIdAndStatut(
                tontine.getId(), Membre.Statut.ACTIF);
        if (membresActifs.isEmpty()) {
            throw new IllegalStateException("Aucun membre actif pour répartir l'aide");
        }
        int n = membresActifs.size();
        SimulationAideResponse sim = RubriqueAideService.calculer(rubrique, n);
        BigDecimal total = sim.total();
        BigDecimal part  = sim.partParMembre();

        // Snapshot
        aide.setModeCalcul(rubrique.getModeCalcul());
        aide.setMontantReference(rubrique.getMontantReference());
        aide.setNbMembresBase(n);
        aide.setPartParMembre(part);
        aide.setMontantAccorde(total);
        aide.setPrefinance(prefinance);
        aide.setValidePar(admin);
        aide.setDateValidation(OffsetDateTime.now());

        FondsAide fonds = fondsAideRepository.findByTontineId(tontine.getId())
                .orElseThrow(() -> new IllegalStateException("Fonds d'aide introuvable pour la tontine"));

        // Une contribution par membre ; le reliquat d'arrondi tombe sur le dernier
        List<ContributionFondsAide> contributions = new ArrayList<>(n);
        BigDecimal cumul = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            BigDecimal montantPart = (i < n - 1) ? part : total.subtract(cumul);
            cumul = cumul.add(part);
            contributions.add(ContributionFondsAide.builder()
                    .fondsAide(fonds)
                    .membre(membresActifs.get(i))
                    .montant(montantPart)
                    .aide(aide)
                    .build());
        }
        contributionFondsAideRepository.saveAll(contributions);

        if (prefinance) {
            fonds.setSolde(fonds.getSolde().subtract(total));
            fondsAideRepository.save(fonds);
            mouvementFondsAideRepository.save(MouvementFondsAide.builder()
                    .fondsAide(fonds)
                    .typeMouvement(MouvementFondsAide.TypeMouvement.DECAISSEMENT)
                    .montant(total)
                    .soldeApres(fonds.getSolde())
                    .aide(aide)
                    .description("Préfinancement aide %s — %s"
                            .formatted(rubrique.getLibelle(), aide.getMembre().getMatricule()))
                    .build());
            aide.setStatut(Aide.Statut.PAYEE);
        } else {
            aide.setStatut(Aide.Statut.VALIDEE);
        }

        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifier(aide.getMembre().getUser(),
                prefinance ? Notification.Type.AIDE_PAYEE : Notification.Type.AIDE_VALIDEE,
                prefinance ? "Aide versée" : "Aide approuvée",
                (prefinance
                        ? "Votre aide « %s » de %s FCFA a été versée (préfinancée par la trésorerie)."
                        : "Votre aide « %s » a été approuvée. Montant : %s FCFA, en cours de collecte.")
                        .formatted(rubrique.getLibelle(), total),
                aide.getId(), "AIDE");

        return response;
    }

    /**
     * Verse au bénéficiaire une aide du barème approuvée mais non préfinancée
     * (état VALIDEE) : décaisse le total du fonds sans régénérer de contributions.
     */
    @Transactional
    public AideResponse verserAide(UUID id) {
        Aide aide = aideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));

        if (aide.getRubrique() == null) {
            throw new IllegalArgumentException("Aide non issue du barème : utilisez « marquer payée »");
        }
        if (aide.getStatut() != Aide.Statut.VALIDEE) {
            throw new IllegalArgumentException("Seule une aide approuvée (non préfinancée) peut être versée");
        }

        Tontine tontine = aide.getMembre().getTontine();
        FondsAide fonds = fondsAideRepository.findByTontineId(tontine.getId())
                .orElseThrow(() -> new IllegalStateException("Fonds d'aide introuvable pour la tontine"));

        BigDecimal total = aide.getMontantAccorde();
        fonds.setSolde(fonds.getSolde().subtract(total));
        fondsAideRepository.save(fonds);
        mouvementFondsAideRepository.save(MouvementFondsAide.builder()
                .fondsAide(fonds)
                .typeMouvement(MouvementFondsAide.TypeMouvement.DECAISSEMENT)
                .montant(total)
                .soldeApres(fonds.getSolde())
                .aide(aide)
                .description("Versement aide %s — %s"
                        .formatted(aide.getRubrique().getLibelle(), aide.getMembre().getMatricule()))
                .build());
        aide.setStatut(Aide.Statut.PAYEE);

        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifier(aide.getMembre().getUser(),
                Notification.Type.AIDE_PAYEE, "Aide versée",
                "Votre aide « %s » de %s FCFA a été versée."
                        .formatted(aide.getRubrique().getLibelle(), total),
                aide.getId(), "AIDE");

        return response;
    }

    private void genererContributionsALaBeneficiation(Aide aide, FondsAide fonds) {
        List<Membre> membresActifs = membreRepository.findAllByTontineIdAndStatut(
                fonds.getTontine().getId(), Membre.Statut.ACTIF);

        if (membresActifs.isEmpty()) return;

        BigDecimal partParMembre = aide.getMontantAccorde()
                .divide(BigDecimal.valueOf(membresActifs.size()), 2, RoundingMode.HALF_UP);

        short annee = (short) LocalDate.now().getYear();

        List<ContributionFondsAide> contributions = membresActifs.stream()
                .map(m -> ContributionFondsAide.builder()
                        .fondsAide(fonds)
                        .membre(m)
                        .montant(partParMembre)
                        .annee(annee)
                        .aide(aide)
                        .build())
                .toList();

        contributionFondsAideRepository.saveAll(contributions);
    }
}