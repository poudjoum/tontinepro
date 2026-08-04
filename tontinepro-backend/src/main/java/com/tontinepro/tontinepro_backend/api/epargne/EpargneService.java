package com.tontinepro.tontinepro_backend.api.epargne;

import com.tontinepro.tontinepro_backend.api.epargne.dto.CompteEpargneResponse;
import com.tontinepro.tontinepro_backend.api.epargne.dto.DepotRequest;
import com.tontinepro.tontinepro_backend.api.epargne.dto.MouvementEpargneResponse;
import com.tontinepro.tontinepro_backend.api.epargne.dto.RetraitRequest;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.MouvementEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.MouvementEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EpargneService {

    private final CompteEpargneRepository compteRepository;
    private final MouvementEpargneRepository mouvementRepository;
    private final MembreRepository membreRepository;
    private final TontineRepository tontineRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public CompteEpargneResponse getMonCompte(String email) {
        return getMonCompte(email, null);
    }

    @Transactional(readOnly = true)
    public CompteEpargneResponse getMonCompte(String email, UUID tontineId) {
        var compte = tontineId != null
                ? compteRepository.findByMembreUserEmailAndMembreTontineId(email, tontineId)
                : compteRepository.findByMembreUserEmail(email);
        return compte
                .map(CompteEpargneResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte épargne associé à ce membre pour cette tontine"));
    }

    @Transactional(readOnly = true)
    public CompteEpargneResponse getCompteByMembre(UUID membreId) {
        return compteRepository.findByMembreId(membreId)
                .map(CompteEpargneResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte épargne pour le membre : " + membreId));
    }

    @Transactional(readOnly = true)
    public List<CompteEpargneResponse> getAllComptes(UUID tontineId) {
        List<CompteEpargne> comptes = tontineId != null
                ? compteRepository.findAllByMembreTontineId(tontineId)
                : compteRepository.findAll();
        return comptes.stream().map(CompteEpargneResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MouvementEpargneResponse> getHistorique(String email) {
        return getHistorique(email, null);
    }

    @Transactional(readOnly = true)
    public List<MouvementEpargneResponse> getHistorique(String email, UUID tontineId) {
        CompteEpargne compte = (tontineId != null
                ? compteRepository.findByMembreUserEmailAndMembreTontineId(email, tontineId)
                : compteRepository.findByMembreUserEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte épargne associé à ce membre pour cette tontine"));
        return mouvementRepository.findAllByCompteIdOrderByCreatedAtDesc(compte.getId())
                .stream().map(MouvementEpargneResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MouvementEpargneResponse> getHistoriqueByMembre(UUID membreId) {
        CompteEpargne compte = compteRepository.findByMembreId(membreId)
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte épargne pour le membre : " + membreId));
        return mouvementRepository.findAllByCompteIdOrderByCreatedAtDesc(compte.getId())
                .stream().map(MouvementEpargneResponse::from).toList();
    }

    @Transactional
    public CompteEpargneResponse depot(String email, DepotRequest request) {
        CompteEpargne compte = compteRepository.findByMembreUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte épargne associé à ce membre"));

        if (compte.getMembre().getStatut() != Membre.Statut.ACTIF) {
            throw new IllegalArgumentException("Seuls les membres actifs peuvent effectuer un dépôt");
        }

        compte.setSolde(compte.getSolde().add(request.montant()));
        compteRepository.save(compte);

        mouvementRepository.save(MouvementEpargne.builder()
                .compte(compte)
                .typeMouvement(MouvementEpargne.TypeMouvement.DEPOT)
                .montant(request.montant())
                .soldeApres(compte.getSolde())
                .reference(request.reference())
                .build());

        notificationService.notifier(compte.getMembre().getUser(),
                Notification.Type.EPARGNE_DEPOT,
                "Dépôt sur votre épargne — %s FCFA".formatted(request.montant()),
                "Dépôt de %s FCFA enregistré. Nouveau solde : %s FCFA."
                        .formatted(request.montant(), compte.getSolde()),
                compte.getId(), "EPARGNE");

        return CompteEpargneResponse.from(compte);
    }

    @Transactional
    public CompteEpargneResponse retrait(String email, RetraitRequest request) {
        CompteEpargne compte = compteRepository.findByMembreUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Aucun compte épargne associé à ce membre"));

        if (compte.getMembre().getStatut() != Membre.Statut.ACTIF) {
            throw new IllegalArgumentException("Seuls les membres actifs peuvent effectuer un retrait");
        }

        if (compte.getSolde().compareTo(request.montant()) < 0) {
            throw new IllegalArgumentException(
                    "Solde insuffisant (%s disponible, %s demandé)"
                            .formatted(compte.getSolde(), request.montant()));
        }

        compte.setSolde(compte.getSolde().subtract(request.montant()));
        compteRepository.save(compte);

        mouvementRepository.save(MouvementEpargne.builder()
                .compte(compte)
                .typeMouvement(MouvementEpargne.TypeMouvement.RETRAIT)
                .montant(request.montant())
                .soldeApres(compte.getSolde())
                .reference(request.reference())
                .build());

        notificationService.notifier(compte.getMembre().getUser(),
                Notification.Type.EPARGNE_RETRAIT,
                "Retrait sur votre épargne — %s FCFA".formatted(request.montant()),
                "Retrait de %s FCFA effectué. Nouveau solde : %s FCFA."
                        .formatted(request.montant(), compte.getSolde()),
                compte.getId(), "EPARGNE");

        return CompteEpargneResponse.from(compte);
    }

    /**
     * Distribue les intérêts sur tous les comptes épargne d'une tontine.
     * Le taux utilisé est {@code tontine.tauxInteretEpargne} (en %).
     * Seuls les comptes avec un solde > 0 sont crédités.
     *
     * @return nombre de comptes crédités
     */
    @Transactional
    public int distribuerInterets(UUID tontineId) {
        Tontine tontine = tontineRepository.findById(tontineId)
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable : " + tontineId));

        if (tontine.getTauxInteretEpargne().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Le taux d'intérêt épargne est à 0 % pour cette tontine");
        }

        List<CompteEpargne> comptes = compteRepository.findAllByMembreTontineId(tontineId)
                .stream()
                .filter(c -> c.getSolde().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        for (CompteEpargne compte : comptes) {
            BigDecimal interet = compte.getSolde()
                    .multiply(tontine.getTauxInteretEpargne())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            compte.setSolde(compte.getSolde().add(interet));
            compteRepository.save(compte);

            mouvementRepository.save(MouvementEpargne.builder()
                    .compte(compte)
                    .typeMouvement(MouvementEpargne.TypeMouvement.INTERET)
                    .montant(interet)
                    .soldeApres(compte.getSolde())
                    .reference("Intérêts " + tontine.getTauxInteretEpargne() + "%")
                    .build());

            notificationService.notifier(compte.getMembre().getUser(),
                    Notification.Type.EPARGNE_INTERETS,
                    "Intérêts crédités sur votre épargne — %s FCFA".formatted(interet),
                    "Intérêts de %s FCFA (%s%%) crédités. Nouveau solde : %s FCFA."
                            .formatted(interet, tontine.getTauxInteretEpargne(), compte.getSolde()),
                    compte.getId(), "EPARGNE");
        }

        return comptes.size();
    }
}
