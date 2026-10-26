package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.session.*;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AbandonLotServiceTest {

    private final SessionTontineRepository sessionRepo = mock(SessionTontineRepository.class);
    private final ParticipationLotRepository participationRepo = mock(ParticipationLotRepository.class);
    private final PartLotRepository partRepo = mock(PartLotRepository.class);
    private final AbandonLotRepository abandonRepo = mock(AbandonLotRepository.class);
    private final CotisationRepository cotisationRepo = mock(CotisationRepository.class);
    private final LotService lotService = mock(LotService.class);

    private AbandonLotService service;
    private Tontine tontine;
    private SessionTontine session;
    private Membre membre;
    private ParticipationLot participation;
    private OrdreBeneficiaire tour;
    private final List<Cotisation> cotisations = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new AbandonLotService(sessionRepo, participationRepo, partRepo, abandonRepo, cotisationRepo, lotService);
        tontine = new Tontine();
        tontine.setId(UUID.randomUUID());
        tontine.setMode(Tontine.ModeTontine.A_LOT);
        session = SessionTontine.builder().id(UUID.randomUUID()).tontine(tontine)
                .statut(SessionTontine.Statut.EN_COURS).figee(true)
                .dateDebut(LocalDate.now().minusMonths(6).withDayOfMonth(1))
                .dateFin(LocalDate.now().plusMonths(6)).nombreMembres(10).build();
        membre = new Membre();
        membre.setId(UUID.randomUUID());
        participation = ParticipationLot.builder().id(UUID.randomUUID()).session(session).membre(membre)
                .montantMensuel(new BigDecimal("10000")).build();
        tour = OrdreBeneficiaire.builder().id(UUID.randomUUID()).session(session).ordre(8).build();

        when(sessionRepo.findById(session.getId())).thenReturn(Optional.of(session));
        when(participationRepo.findBySessionIdAndMembreId(session.getId(), membre.getId()))
                .thenReturn(Optional.of(participation));
        when(cotisationRepo.findAllByMembreId(membre.getId())).thenReturn(cotisations);
        when(abandonRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void cotisation(int moisDepuisDebut, Cotisation.Statut statut) {
        LocalDate d = session.getDateDebut().plusMonths(moisDepuisDebut);
        cotisations.add(Cotisation.builder().membre(membre).tontine(tontine)
                .mois((short) d.getMonthValue()).annee((short) d.getYear())
                .montant(new BigDecimal("10000")).statut(statut).build());
    }

    private PartLot part(String cagnotte, boolean verse) {
        PartLot p = PartLot.builder().id(UUID.randomUUID()).ordreBeneficiaire(tour).membre(membre)
                .montantMensuel(new BigDecimal("10000")).partCagnotte(new BigDecimal(cagnotte)).verse(verse).build();
        when(partRepo.findAllByOrdreBeneficiaireSessionId(session.getId())).thenReturn(List.of(p));
        return p;
    }

    @Test
    void avantSonTour_avoirGeleEgalAuCotiseEtPartAnnulee() {
        for (int m = 0; m < 4; m++) cotisation(m, Cotisation.Statut.PAYEE);
        cotisation(6, Cotisation.Statut.EN_ATTENTE);   // mois de l'abandon, pas payé
        PartLot p = part("100000", false);

        AbandonLot a = service.declarer(session.getId(), membre.getId(), LocalDate.now(), "Déménagement");

        assertThat(a.getMoisCotises()).isEqualTo(4);
        assertThat(a.getTotalCotise()).isEqualByComparingTo("40000");
        assertThat(a.getTotalRecu()).isEqualByComparingTo("0");
        assertThat(a.getSolde()).isEqualByComparingTo("40000");
        assertThat(a.getStatut()).isEqualTo(AbandonLot.Statut.AVOIR_GELE);
        assertThat(p.isAnnulee()).isTrue();
        assertThat(participation.isAbandonnee()).isTrue();
        verify(lotService).majTour(tour);
        verify(cotisationRepo).deleteAll(argThatTaille(1));
    }

    @Test
    void apresSonTour_detteEgaleAuRecuMoinsLeCotise() {
        for (int m = 0; m < 3; m++) cotisation(m, Cotisation.Statut.PAYEE);
        part("100000", true);

        AbandonLot a = service.declarer(session.getId(), membre.getId(), LocalDate.now(), null);

        assertThat(a.getSolde()).isEqualByComparingTo("-70000");
        assertThat(a.getStatut()).isEqualTo(AbandonLot.Statut.DETTE);
        assertThat(a.resteARembourser()).isEqualByComparingTo("70000");
    }

    @Test
    void laDetteSeSoldeParRemboursements() {
        AbandonLot a = abandon(new BigDecimal("-70000"), AbandonLot.Statut.DETTE);

        service.rembourser(session.getId(), a.getId(), new BigDecimal("50000"));
        assertThat(a.getStatut()).isEqualTo(AbandonLot.Statut.DETTE);
        assertThat(a.resteARembourser()).isEqualByComparingTo("20000");

        assertThatThrownBy(() -> service.rembourser(session.getId(), a.getId(), new BigDecimal("30000")))
                .isInstanceOf(IllegalArgumentException.class);

        service.rembourser(session.getId(), a.getId(), new BigDecimal("20000"));
        assertThat(a.getStatut()).isEqualTo(AbandonLot.Statut.DETTE_SOLDEE);
    }

    @Test
    void lAvoirNeSeRestitueQuEnFinDeSession() {
        AbandonLot a = abandon(new BigDecimal("40000"), AbandonLot.Statut.AVOIR_GELE);
        PartLot enAttente = part("500000", false);

        assertThatThrownBy(() -> service.restituer(session.getId(), a.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fin de session");

        enAttente.setVerse(true);
        service.restituer(session.getId(), a.getId());
        assertThat(a.getStatut()).isEqualTo(AbandonLot.Statut.AVOIR_RESTITUE);
        assertThat(a.getDateRestitution()).isEqualTo(LocalDate.now());
    }

    @Test
    void annulerRetablitLesParts() {
        AbandonLot a = abandon(new BigDecimal("40000"), AbandonLot.Statut.AVOIR_GELE);
        PartLot p = part("100000", false);
        p.setAnnulee(true);
        participation.setAbandonnee(true);

        service.annuler(session.getId(), a.getId());

        assertThat(p.isAnnulee()).isFalse();
        assertThat(participation.isAbandonnee()).isFalse();
        verify(abandonRepo).delete(a);
    }

    @Test
    void refuseUneDateFuture() {
        assertThatThrownBy(() -> service.declarer(session.getId(), membre.getId(), LocalDate.now().plusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AbandonLot abandon(BigDecimal solde, AbandonLot.Statut statut) {
        AbandonLot a = AbandonLot.builder().id(UUID.randomUUID()).session(session).membre(membre)
                .dateAbandon(LocalDate.now()).totalCotise(BigDecimal.ZERO).totalRecu(BigDecimal.ZERO)
                .solde(solde).statut(statut).build();
        when(abandonRepo.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    private static List<Cotisation> argThatTaille(int taille) {
        return argThat(l -> l != null && ((List<?>) l).size() == taille);
    }
}
