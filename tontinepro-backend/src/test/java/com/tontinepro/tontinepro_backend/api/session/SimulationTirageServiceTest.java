package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.session.dto.OrdreBeneficiaireResponse;
import com.tontinepro.tontinepro_backend.api.session.dto.SimulationTirageRequest;
import com.tontinepro.tontinepro_backend.api.session.dto.SimulationTirageResponse;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontine;
import com.tontinepro.tontinepro_backend.domain.session.SessionTontineRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de la méthode {@code SessionService.simulerTirage()}.
 * Vérifie les cas nominaux, les erreurs de validation, le calcul des dates,
 * le numéro de session, l'indicateur déjà figé, et le mélange aléatoire.
 */
@ExtendWith(MockitoExtension.class)
class SimulationTirageServiceTest {

    @Mock
    private TontineRepository tontineRepository;

    @Mock
    private MembreRepository membreRepository;

    @Mock
    private SessionTontineRepository sessionRepository;

    @Mock
    private PeriodiciteService periodiciteService;

    private SessionService sessionService;
    private Tontine tontine;
    private final UUID tontineId = UUID.randomUUID();
    private final LocalDate dateDebut = LocalDate.of(2027, 1, 15);

    @BeforeEach
    void setUp() {
        // Construire le service avec les dépendances mockées.
        // Seuls tontineRepository, membreRepository, sessionRepository et periodiciteService
        // sont utilisés par simulerTirage(). Les autres sont null.
        sessionService = new SessionService(
                sessionRepository,
                null,  // ordreBeneficiaireRepository
                tontineRepository,
                membreRepository,
                null,  // cotisationRepository
                null,  // sanctionRepository
                periodiciteService,
                null,  // notificationService
                null,  // rapportEmailService
                null,  // compteEpargneRepository
                null,  // pretRepository
                null,  // fondsAideRepository
                null,  // versementAnterieurRepository
                null,  // mouvementFondsAideRepository
                null,  // contributionFondsAideRepository
                null,  // retenueFondCaisse
                null,  // lotService
                null,  // participationLotRepository
                null,  // partLotRepository
                null,  // abandonLotRepository
                null   // arriereService
        );

        tontine = new Tontine();
        tontine.setId(tontineId);
        tontine.setMode(Tontine.ModeTontine.CLASSIQUE);
        tontine.setTypeReglePeriodicite(Tontine.TypeReglePeriodicite.MENSUEL_JOUR_FIXE);
    }

    /* ─── Helper factories ─── */

    private Membre membre(String prenom, String nom) {
        Membre m = new Membre();
        m.setId(UUID.randomUUID());
        m.setPrenom(prenom);
        m.setNom(nom);
        m.setMatricule("MAT-" + prenom);
        m.setStatut(Membre.Statut.ACTIF);
        m.setTypeParticipation(Membre.TypeParticipation.TONTINE);
        return m;
    }

    private SimulationTirageRequest request() {
        return new SimulationTirageRequest(tontineId, dateDebut, null);
    }

    private void givenTontineClassique() {
        when(tontineRepository.findById(tontineId)).thenReturn(Optional.of(tontine));
    }

    private void givenAucuneSession() {
        when(sessionRepository.findTopByTontineIdOrderByNumeroDesc(tontineId))
                .thenReturn(Optional.empty());
        when(sessionRepository.existsByTontineIdAndNumero(eq(tontineId), anyInt()))
                .thenReturn(false);
    }

    private void givenDatesCalculees(int nombreMembres) {
        LocalDate dateFin = dateDebut.plusMonths(nombreMembres - 1);
        when(periodiciteService.calculerDateFin(eq(tontine), eq(dateDebut), eq(nombreMembres)))
                .thenReturn(dateFin);

        List<LocalDate> dates = new ArrayList<>();
        for (int i = 0; i < nombreMembres; i++) {
            dates.add(dateDebut.plusMonths(i));
        }
        when(periodiciteService.calculerDatesBenefice(eq(tontine), eq(dateDebut), eq(nombreMembres)))
                .thenReturn(dates);
    }

    /* ─── Tests : erreurs de validation ─── */

    @Test
    void simulerTirage_tontineIntrouvable_lanceException() {
        when(tontineRepository.findById(tontineId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.simulerTirage(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tontine introuvable");

        verify(membreRepository, never()).findAllByTontineIdAndStatutAndTypeParticipation(any(), any(), any());
    }

    @Test
    void simulerTirage_modeLot_lanceException() {
        tontine.setMode(Tontine.ModeTontine.A_LOT);
        givenTontineClassique();

        assertThatThrownBy(() -> sessionService.simulerTirage(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("classiques");
    }


    @Test
    void simulerTirage_aucunMembreActif_lanceException() {
        givenTontineClassique();
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> sessionService.simulerTirage(request()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucun membre actif");
    }

    /* ─── Tests : cas nominaux ─── */

    @Test
    void simulerTirage_unSeulMembre_retourneUnBeneficiaire() {
        Membre m1 = membre("Aminata", "Diallo");
        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.tontineId()).isEqualTo(tontineId);
        assertThat(response.prochainNumeroSession()).isEqualTo(1);
        assertThat(response.dateDebut()).isEqualTo(dateDebut);
        assertThat(response.nombreMembres()).isEqualTo(1);
        assertThat(response.dejaFige()).isFalse();
        assertThat(response.beneficiaires()).hasSize(1);

        OrdreBeneficiaireResponse premier = response.beneficiaires().get(0);
        assertThat(premier.membreId()).isEqualTo(m1.getId());
        assertThat(premier.membrePrenom()).isEqualTo("Aminata");
        assertThat(premier.membreNom()).isEqualTo("Diallo");
        assertThat(premier.ordre()).isEqualTo(1);
        assertThat(premier.id()).isNull();
        assertThat(premier.beneficie()).isFalse();
    }

    @Test
    void simulerTirage_plusieursMembres_retourneTousLesBeneficiaires() {
        Membre m1 = membre("Aminata", "Diallo");
        Membre m2 = membre("Ibrahim", "Traore");
        Membre m3 = membre("Fatou", "Konate");
        List<Membre> membres = List.of(m1, m2, m3);

        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(3);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(membres);

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.nombreMembres()).isEqualTo(3);
        assertThat(response.beneficiaires()).hasSize(3);
        assertThat(response.dateFin()).isEqualTo(dateDebut.plusMonths(2));

        // Les ordres sont 1, 2, 3
        assertThat(response.beneficiaires().get(0).ordre()).isEqualTo(1);
        assertThat(response.beneficiaires().get(1).ordre()).isEqualTo(2);
        assertThat(response.beneficiaires().get(2).ordre()).isEqualTo(3);

        // Tous les membres sont présents (ordre aléatoire)
        List<UUID> idsRetournes = response.beneficiaires().stream()
                .map(OrdreBeneficiaireResponse::membreId)
                .toList();
        assertThat(idsRetournes).containsExactlyInAnyOrder(m1.getId(), m2.getId(), m3.getId());
    }

    @Test
    void simulerTirage_champsNonPersistes_sontNuls() {
        Membre m1 = membre("Aminata", "Diallo");
        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        OrdreBeneficiaireResponse beneficiaire = response.beneficiaires().get(0);
        assertThat(beneficiaire.id()).isNull();
        assertThat(beneficiaire.montantRecu()).isNull();
        assertThat(beneficiaire.montantAvance()).isNull();
        assertThat(beneficiaire.dateAvance()).isNull();
        assertThat(beneficiaire.montantCash()).isNull();
        assertThat(beneficiaire.montantOm()).isNull();
        assertThat(beneficiaire.montantMomo()).isNull();
        assertThat(beneficiaire.refOm()).isNull();
        assertThat(beneficiaire.refMomo()).isNull();
    }

    /* ─── Tests : numéro de session ─── */

    @Test
    void simulerTirage_premiereSession_numeroEstUn() {
        Membre m1 = membre("Aminata", "Diallo");
        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.prochainNumeroSession()).isEqualTo(1);
    }

    @Test
    void simulerTirage_sessionExistante_prochainNumeroIncremente() {
        Membre m1 = membre("Aminata", "Diallo");
        SessionTontine sessionExistante = SessionTontine.builder()
                .id(UUID.randomUUID())
                .tontine(tontine)
                .numero(3)
                .build();

        givenTontineClassique();
        when(sessionRepository.findTopByTontineIdOrderByNumeroDesc(tontineId))
                .thenReturn(Optional.of(sessionExistante));
        when(sessionRepository.existsByTontineIdAndNumero(eq(tontineId), eq(4)))
                .thenReturn(false);
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.prochainNumeroSession()).isEqualTo(4);
        assertThat(response.dejaFige()).isFalse();
    }

    /* ─── Tests : déjà figé ─── */

    @Test
    void simulerTirage_sessionDejaFigee_dejaFigeEstVrai() {
        Membre m1 = membre("Aminata", "Diallo");

        givenTontineClassique();
        when(sessionRepository.findTopByTontineIdOrderByNumeroDesc(tontineId))
                .thenReturn(Optional.empty());
        when(sessionRepository.existsByTontineIdAndNumero(eq(tontineId), eq(1)))
                .thenReturn(true);
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.dejaFige()).isTrue();
    }

    @Test
    void simulerTirage_sessionPasFigee_dejaFigeEstFaux() {
        Membre m1 = membre("Aminata", "Diallo");
        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.dejaFige()).isFalse();
    }

    /* ─── Tests : dates de bénéfice ─── */

    @Test
    void simulerTirage_datesBenefice_correctes() {
        Membre m1 = membre("Aminata", "Diallo");
        Membre m2 = membre("Ibrahim", "Traore");
        List<Membre> membres = List.of(m1, m2);

        LocalDate d1 = LocalDate.of(2027, 1, 15);
        LocalDate d2 = LocalDate.of(2027, 2, 15);
        List<LocalDate> datesBenefice = List.of(d1, d2);

        givenTontineClassique();
        givenAucuneSession();
        when(periodiciteService.calculerDateFin(eq(tontine), eq(dateDebut), eq(2)))
                .thenReturn(d2);
        when(periodiciteService.calculerDatesBenefice(eq(tontine), eq(dateDebut), eq(2)))
                .thenReturn(datesBenefice);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(membres);

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.beneficiaires()).hasSize(2);
        List<LocalDate> datesRetournees = response.beneficiaires().stream()
                .map(OrdreBeneficiaireResponse::dateBenefice)
                .toList();
        assertThat(datesRetournees).isNotEmpty();
    }

    /* ─── Tests : informations du membre ─── */

    @Test
    void simulerTirage_statutMembre_inclusDansLaReponse() {
        Membre m1 = membre("Aminata", "Diallo");
        m1.setStatut(Membre.Statut.ACTIF);
        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.beneficiaires().get(0).membreStatut()).isEqualTo("ACTIF");
    }

    @Test
    void simulerTirage_matriculeMembre_inclusDansLaReponse() {
        Membre m1 = membre("Aminata", "Diallo");
        m1.setMatricule("TON-2027-001");
        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        SimulationTirageResponse response = sessionService.simulerTirage(request());

        assertThat(response.beneficiaires().get(0).membreMatricule())
                .isEqualTo("TON-2027-001");
    }

    /* ─── Tests : cibleMembres optionnel ─── */

    @Test
    void simulerTirage_cibleMembres_optionnel() {
        Membre m1 = membre("Aminata", "Diallo");
        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        // Avec cibleMembres = 10 (optionnel, ignoré dans la simulation)
        SimulationTirageRequest reqAvecCible = new SimulationTirageRequest(tontineId, dateDebut, 10);
        SimulationTirageResponse response = sessionService.simulerTirage(reqAvecCible);

        // Le nombre réel de membres actifs est retourné, pas la cible
        assertThat(response.nombreMembres()).isEqualTo(1);
    }

    /* ─── Tests : mélange aléatoire ─── */

    @Test
    void simulerTirage_melangeAleatoire_ordreVarie() {
        Membre m1 = membre("Aminata", "Diallo");
        Membre m2 = membre("Ibrahim", "Traore");
        Membre m3 = membre("Fatou", "Konate");
        List<Membre> membres = List.of(m1, m2, m3);

        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(3);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(membres);

        // Exécuter 20 fois et vérifier que l'ordre varie (probabiliste)
        boolean ordreVarie = false;
        List<String> premierOrdre = null;

        for (int i = 0; i < 20; i++) {
            SimulationTirageResponse response = sessionService.simulerTirage(request());
            List<String> ordreActuel = response.beneficiaires().stream()
                    .map(OrdreBeneficiaireResponse::membrePrenom)
                    .toList();

            if (premierOrdre != null && !ordreActuel.equals(premierOrdre)) {
                ordreVarie = true;
                break;
            }
            if (premierOrdre == null) {
                premierOrdre = ordreActuel;
            }
        }

        // Avec 20 tirages sur 3 membres, la probabilité que l'ordre reste identique est quasi nulle
        assertThat(ordreVarie).isTrue();
    }

    /* ─── Tests : read-only (pas de persistance) ─── */

    @Test
    void simulerTirage_readOnly_nePersisteRien() {
        Membre m1 = membre("Aminata", "Diallo");
        givenTontineClassique();
        givenAucuneSession();
        givenDatesCalculees(1);
        when(membreRepository.findAllByTontineIdAndStatutAndTypeParticipation(
                eq(tontineId), eq(Membre.Statut.ACTIF), eq(Membre.TypeParticipation.TONTINE)))
                .thenReturn(List.of(m1));

        sessionService.simulerTirage(request());

        // Vérifier qu'aucune méthode de sauvegarde n'a été appelée sur les repositories
        verify(sessionRepository, never()).save(any());
        verify(membreRepository, never()).save(any());
        verify(tontineRepository, never()).save(any());
    }
}
