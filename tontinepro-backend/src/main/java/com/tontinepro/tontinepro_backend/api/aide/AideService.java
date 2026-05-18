package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.AideResponse;
import com.tontinepro.tontinepro_backend.api.aide.dto.DemandeAideRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.RejeterAideRequest;
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
    private final NotificationService notificationService;

    @Transactional
    public AideResponse soumettreDemande(String email, DemandeAideRequest request) {
        Membre membre = membreRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));

        if (membre.getStatut() != Membre.Statut.ACTIF) {
            throw new IllegalArgumentException("Seuls les membres actifs peuvent soumettre une demande d'aide");
        }

        Aide aide = Aide.builder()
                .membre(membre)
                .typeAide(request.typeAide())
                .montantDemande(request.montantDemande())
                .motif(request.motif())
                .justificatifUrl(request.justificatifUrl())
                .build();

        AideResponse response = AideResponse.from(aideRepository.save(aide));

        notificationService.notifierAdmins(
                Notification.Type.AIDE_SOUMISE,
                "Nouvelle demande d'aide",
                "Le membre %s %s a soumis une demande d'aide (%s) de %s FCFA."
                        .formatted(membre.getNom(), membre.getPrenom(),
                                request.typeAide(), request.montantDemande()),
                response.id(), "AIDE");

        return response;
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
        return membreRepository.findByUserEmail(email)
                .map(m -> aideRepository.findAllByMembreId(m.getId())
                        .stream().map(AideResponse::from).toList())
                .orElseThrow(() -> new IllegalArgumentException("Aucun profil membre associé à ce compte"));
    }

    @Transactional
    public AideResponse valider(UUID id, String adminEmail, ValiderAideRequest request) {
        Aide aide = aideRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande d'aide introuvable : " + id));

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