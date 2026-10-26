package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.domain.cotisation.CotisationRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.session.*;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LotServiceVersementTest {

    private final PartLotRepository partRepo = mock(PartLotRepository.class);
    private final OrdreBeneficiaireRepository obRepo = mock(OrdreBeneficiaireRepository.class);
    private final RetenueFondCaisse retenue = mock(RetenueFondCaisse.class);

    private LotService service;
    private SessionTontine session;
    private Tontine tontine;
    private OrdreBeneficiaire tour;

    @BeforeEach
    void setUp() {
        service = new LotService(mock(SessionTontineRepository.class), mock(ParticipationLotRepository.class),
                obRepo, partRepo, mock(MembreRepository.class), retenue, mock(PeriodiciteService.class),
                mock(AbandonLotRepository.class), mock(CotisationRepository.class));
        tontine = new Tontine();
        tontine.setId(UUID.randomUUID());
        tontine.setMode(Tontine.ModeTontine.A_LOT);
        session = SessionTontine.builder().id(UUID.randomUUID()).tontine(tontine).figee(true)
                .dateDebut(LocalDate.of(2026, 1, 1)).nombreMembres(10).build();
        tour = OrdreBeneficiaire.builder().id(UUID.randomUUID()).session(session).ordre(1)
                .dateBenefice(LocalDate.of(2026, 3, 1)).build();
        when(partRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private PartLot part(String partCagnotte) {
        Membre m = new Membre();
        m.setId(UUID.randomUUID());
        m.setNom("N");
        m.setPrenom("P");
        PartLot p = PartLot.builder().id(UUID.randomUUID()).ordreBeneficiaire(tour).membre(m)
                .montantMensuel(new BigDecimal("10000")).partCagnotte(new BigDecimal(partCagnotte)).build();
        when(partRepo.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void verseLaPartMoinsLeResteDuDuFondDeCaisse() {
        PartLot a = part("100000");
        PartLot b = part("400000");
        when(partRepo.findAllByOrdreBeneficiaireId(tour.getId())).thenReturn(List.of(a, b));
        when(retenue.resteDu(eq(tontine), eq(a.getMembre().getId()), eq((short) 2026))).thenReturn(new BigDecimal("12000"));

        service.verserPart(session.getId(), a.getId());

        assertThat(a.isVerse()).isTrue();
        assertThat(a.getMontantRetenue()).isEqualByComparingTo("12000");
        assertThat(a.getMontantVerse()).isEqualByComparingTo("88000");
        assertThat(a.getAnneeRetenue()).isEqualTo((short) 2026);
        assertThat(tour.isBeneficie()).isFalse();   // l'autre part reste à verser
    }

    @Test
    void leTourEstCloQuandToutesLesPartsSontVersees() {
        PartLot a = part("100000");
        PartLot b = part("400000");
        when(partRepo.findAllByOrdreBeneficiaireId(tour.getId())).thenReturn(List.of(a, b));
        when(retenue.resteDu(any(), any(), anyShort())).thenReturn(BigDecimal.ZERO);
        when(obRepo.findById(tour.getId())).thenReturn(Optional.of(tour));

        service.verserTour(session.getId(), tour.getId());

        assertThat(tour.isBeneficie()).isTrue();
        assertThat(tour.getMontantRecu()).isEqualByComparingTo("500000");
    }

    @Test
    void neRetientPasDeuxFoisUnMembreAPlusieursTours() {
        PartLot a = part("100000");
        PartLot dejaVersee = PartLot.builder().id(UUID.randomUUID()).membre(a.getMembre())
                .verse(true).montantRetenue(new BigDecimal("12000")).build();
        when(partRepo.findAllByOrdreBeneficiaireId(tour.getId())).thenReturn(List.of(a));
        when(retenue.resteDu(any(), any(), anyShort())).thenReturn(new BigDecimal("12000"));
        when(partRepo.findAllByMembreIdAndVerseTrueAndAnneeRetenue(a.getMembre().getId(), (short) 2026))
                .thenReturn(List.of(dejaVersee));

        service.verserPart(session.getId(), a.getId());

        assertThat(a.getMontantRetenue()).isEqualByComparingTo("0");
        assertThat(a.getMontantVerse()).isEqualByComparingTo("100000");
    }

    @Test
    void laRetenueNeDepassePasLaPart() {
        PartLot a = part("5000");
        when(partRepo.findAllByOrdreBeneficiaireId(tour.getId())).thenReturn(List.of(a));
        when(retenue.resteDu(any(), any(), anyShort())).thenReturn(new BigDecimal("12000"));

        service.verserPart(session.getId(), a.getId());

        assertThat(a.getMontantRetenue()).isEqualByComparingTo("5000");
        assertThat(a.getMontantVerse()).isEqualByComparingTo("0");
    }

    @Test
    void annulerRouvreLeTour() {
        PartLot a = part("100000");
        when(partRepo.findAllByOrdreBeneficiaireId(tour.getId())).thenReturn(List.of(a));
        when(retenue.resteDu(any(), any(), anyShort())).thenReturn(BigDecimal.ZERO);
        service.verserPart(session.getId(), a.getId());
        assertThat(tour.isBeneficie()).isTrue();

        service.annulerVersementPart(session.getId(), a.getId());

        assertThat(a.isVerse()).isFalse();
        assertThat(a.getMontantVerse()).isNull();
        assertThat(tour.isBeneficie()).isFalse();
        assertThat(tour.getMontantRecu()).isNull();
    }

    @Test
    void refuseUnDoubleVersement() {
        PartLot a = part("100000");
        when(partRepo.findAllByOrdreBeneficiaireId(tour.getId())).thenReturn(List.of(a));
        when(retenue.resteDu(any(), any(), anyShort())).thenReturn(BigDecimal.ZERO);
        service.verserPart(session.getId(), a.getId());

        assertThatThrownBy(() -> service.verserPart(session.getId(), a.getId()))
                .isInstanceOf(IllegalStateException.class);
    }
}
