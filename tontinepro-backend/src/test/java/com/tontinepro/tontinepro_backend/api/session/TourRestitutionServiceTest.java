package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
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
import static org.mockito.Mockito.*;

class TourRestitutionServiceTest {

    private final SessionTontineRepository sessionRepo = mock(SessionTontineRepository.class);
    private final TourRestitutionRepository tourRepo = mock(TourRestitutionRepository.class);
    private final TourRestitutionCotisantRepository cotisantRepo = mock(TourRestitutionCotisantRepository.class);
    private final AbandonLotRepository abandonRepo = mock(AbandonLotRepository.class);
    private final MembreRepository membreRepo = mock(MembreRepository.class);
    private final AbandonLotService abandonLotService = mock(AbandonLotService.class);

    private TourRestitutionService service;
    private SessionTontine session;
    private Tontine tontine;
    private Membre cotisant;
    private AbandonLot avoir;
    private final List<TourRestitutionCotisant> cotisants = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new TourRestitutionService(sessionRepo, tourRepo, cotisantRepo, abandonRepo, membreRepo,
                abandonLotService);
        tontine = new Tontine();
        tontine.setId(UUID.randomUUID());
        tontine.setMode(Tontine.ModeTontine.A_LOT);
        session = SessionTontine.builder().id(UUID.randomUUID()).tontine(tontine)
                .statut(SessionTontine.Statut.EN_COURS).dateDebut(LocalDate.of(2026, 2, 22)).build();
        cotisant = membre("Rodrigue");
        Membre parti = membre("Calvine");
        avoir = AbandonLot.builder().id(UUID.randomUUID()).session(session).membre(parti)
                .solde(new BigDecimal("100000")).statut(AbandonLot.Statut.AVOIR_GELE).build();

        when(sessionRepo.findById(session.getId())).thenReturn(Optional.of(session));
        when(abandonRepo.findAllBySessionIdOrderByDateAbandon(session.getId())).thenReturn(List.of(avoir));
        when(membreRepo.findById(cotisant.getId())).thenReturn(Optional.of(cotisant));
        when(tourRepo.save(any())).thenAnswer(i -> {
            TourRestitution t = i.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            when(tourRepo.findBySessionId(session.getId())).thenReturn(Optional.of(t));
            return t;
        });
        when(cotisantRepo.save(any())).thenAnswer(i -> {
            TourRestitutionCotisant c = i.getArgument(0);
            if (c.getId() == null) { c.setId(UUID.randomUUID()); cotisants.add(c); }
            return c;
        });
        when(cotisantRepo.findAllByTourId(any())).thenAnswer(i -> List.copyOf(cotisants));
    }

    private Membre membre(String prenom) {
        Membre m = new Membre();
        m.setId(UUID.randomUUID());
        m.setPrenom(prenom);
        m.setNom("Test");
        m.setTontine(tontine);
        return m;
    }

    private TourRestitution programmer(String montant) {
        return service.programmer(session.getId(), LocalDate.of(2027, 4, 1),
                List.of(new TourRestitutionService.Cotisant(cotisant.getId(), new BigDecimal(montant))));
    }

    @Test
    void programmer_enregistreLaDateEtLesCotisants() {
        TourRestitution tour = programmer("50000");

        assertThat(tour.getDateTour()).isEqualTo(LocalDate.of(2027, 4, 1));
        assertThat(cotisants).singleElement().satisfies(c -> {
            assertThat(c.getMembre()).isSameAs(cotisant);
            assertThat(c.getMontant()).isEqualByComparingTo("50000");
            assertThat(c.isPaye()).isFalse();
        });
    }

    @Test
    void programmer_refuseUnMembreParti() {
        assertThatThrownBy(() -> service.programmer(session.getId(), LocalDate.of(2027, 4, 1),
                List.of(new TourRestitutionService.Cotisant(avoir.getMembre().getId(), new BigDecimal("50000")))))
                .hasMessageContaining("membre parti");
    }

    @Test
    void programmer_sansAvoirGele_refuse() {
        avoir.setStatut(AbandonLot.Statut.DETTE);
        assertThatThrownBy(() -> programmer("50000")).hasMessageContaining("Aucun avoir gelé");
    }

    @Test
    void reprogrammer_auMemeMontant_garderEncaissement_etAutreMontant_leRemetAZero() {
        programmer("50000");
        when(cotisantRepo.findById(cotisants.get(0).getId())).thenReturn(Optional.of(cotisants.get(0)));
        service.encaisser(session.getId(), cotisants.get(0).getId(), true);

        programmer("50000");
        assertThat(cotisants.get(0).isPaye()).isTrue();

        programmer("75000");
        assertThat(cotisants.get(0).isPaye()).isFalse();
    }

    @Test
    void clore_rendTousLesAvoirsEnEntier_memeSiLaCollecteEstInferieure() {
        programmer("50000");
        cotisants.get(0).setPaye(true);
        when(abandonLotService.toursClos(session.getId())).thenReturn(true);

        TourRestitution tour = service.clore(session.getId());

        assertThat(tour.isClos()).isTrue();
        assertThat(tour.getMontantCollecte()).isEqualByComparingTo("50000");
        assertThat(tour.getMontantRestitue()).isEqualByComparingTo("100000");   // la trésorerie avance 50 000
        assertThat(avoir.getStatut()).isEqualTo(AbandonLot.Statut.AVOIR_RESTITUE);
        assertThat(avoir.isRestitueParTour()).isTrue();
    }

    @Test
    void clore_refuseSiUnCotisantNaPasPaye() {
        programmer("50000");
        when(abandonLotService.toursClos(session.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.clore(session.getId())).hasMessageContaining("Rodrigue");
        assertThat(avoir.getStatut()).isEqualTo(AbandonLot.Statut.AVOIR_GELE);
    }

    @Test
    void clore_refuseTantQueLesAutresToursNeSontPasVerses() {
        programmer("50000");
        cotisants.get(0).setPaye(true);
        when(abandonLotService.toursClos(session.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.clore(session.getId())).hasMessageContaining("autres tours");
    }

    @Test
    void rouvrir_regeleLesAvoirsRendusParLeTour() {
        programmer("50000");
        cotisants.get(0).setPaye(true);
        when(abandonLotService.toursClos(session.getId())).thenReturn(true);
        service.clore(session.getId());

        service.rouvrir(session.getId());

        assertThat(avoir.getStatut()).isEqualTo(AbandonLot.Statut.AVOIR_GELE);
        assertThat(avoir.getDateRestitution()).isNull();
        assertThat(avoir.isRestitueParTour()).isFalse();
    }
}
