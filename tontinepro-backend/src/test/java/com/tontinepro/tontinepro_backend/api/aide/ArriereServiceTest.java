package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.ArriereResponse;
import com.tontinepro.tontinepro_backend.domain.aide.*;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

class ArriereServiceTest {

    private final ArriereMembreRepository arriereRepo = mock(ArriereMembreRepository.class);
    private final ReglementArriereRepository reglementRepo = mock(ReglementArriereRepository.class);
    private final MembreRepository membreRepo = mock(MembreRepository.class);
    private final FondsAideRepository fondsRepo = mock(FondsAideRepository.class);
    private final MouvementFondsAideRepository mouvementRepo = mock(MouvementFondsAideRepository.class);

    private ArriereService service;
    private Tontine tontine;
    private Membre rivier;
    private FondsAide fonds;
    private final List<ArriereMembre> arrieres = new ArrayList<>();
    private final List<ReglementArriere> reglements = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new ArriereService(arriereRepo, reglementRepo, membreRepo, fondsRepo, mouvementRepo);
        tontine = new Tontine();
        tontine.setId(UUID.randomUUID());
        rivier = Membre.builder().id(UUID.randomUUID()).tontine(tontine).nom("ESSAA Biboum")
                .prenom("Frank Rivier").matricule("MBR-1").build();
        fonds = FondsAide.builder().tontine(tontine).solde(new BigDecimal("1000")).build();

        when(membreRepo.findById(rivier.getId())).thenReturn(Optional.of(rivier));
        when(fondsRepo.findByTontineId(tontine.getId())).thenReturn(Optional.of(fonds));
        when(arriereRepo.save(any())).thenAnswer(i -> {
            ArriereMembre a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            arrieres.add(a);
            when(arriereRepo.findById(a.getId())).thenReturn(Optional.of(a));
            return a;
        });
        when(arriereRepo.findAllByTontineIdOrderByDateConstatAscCreatedAtAsc(tontine.getId())).thenReturn(arrieres);
        when(reglementRepo.save(any())).thenAnswer(i -> {
            ReglementArriere r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            reglements.add(r);
            when(reglementRepo.findById(r.getId())).thenReturn(Optional.of(r));
            return r;
        });
        when(reglementRepo.findAllByArriereTontineId(tontine.getId())).thenAnswer(i -> List.copyOf(reglements));
        when(reglementRepo.findAllByArriereIdOrderByDateReglementAscCreatedAtAsc(any()))
                .thenAnswer(i -> reglements.stream().filter(r -> r.getArriere().getId().equals(i.getArgument(0))).toList());
        doAnswer(i -> reglements.remove(i.<ReglementArriere>getArgument(0))).when(reglementRepo).delete(any());
    }

    @Test
    void creer_fondsNonCotiseParDefaut() {
        List<ArriereResponse> liste = service.creer(tontine.getId(), rivier.getId(), " ", new BigDecimal("64000"), null);

        assertThat(liste).singleElement().satisfies(a -> {
            assertThat(a.libelle()).isEqualTo("Fonds non cotisé");
            assertThat(a.resteDu()).isEqualByComparingTo("64000");
            assertThat(a.dateConstat()).isEqualTo(LocalDate.now());
        });
    }

    @Test
    void creer_refuseUnMembreDUneAutreTontine() {
        assertThatThrownBy(() -> service.creer(UUID.randomUUID(), rivier.getId(), null, new BigDecimal("1"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void regler_crediteLeFondsEtReduitLeResteDu() {
        UUID id = service.creer(tontine.getId(), rivier.getId(), null, new BigDecimal("64000"), null).get(0).id();

        List<ArriereResponse> liste = service.regler(tontine.getId(), id, new BigDecimal("24000"), null);

        assertThat(liste.get(0).resteDu()).isEqualByComparingTo("40000");
        assertThat(fonds.getSolde()).isEqualByComparingTo("25000");
        ArgumentCaptor<MouvementFondsAide> mvt = ArgumentCaptor.forClass(MouvementFondsAide.class);
        verify(mouvementRepo).save(mvt.capture());
        assertThat(mvt.getValue().getTypeMouvement()).isEqualTo(MouvementFondsAide.TypeMouvement.ARRIERE);
        assertThat(mvt.getValue().getSoldeApres()).isEqualByComparingTo("25000");
    }

    @Test
    void regler_neDepassePasLeResteDu() {
        UUID id = service.creer(tontine.getId(), rivier.getId(), null, new BigDecimal("64000"), null).get(0).id();
        service.regler(tontine.getId(), id, new BigDecimal("60000"), null);

        assertThatThrownBy(() -> service.regler(tontine.getId(), id, new BigDecimal("5000"), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("4000");
    }

    @Test
    void annulerReglement_debiteLeFonds() {
        UUID id = service.creer(tontine.getId(), rivier.getId(), null, new BigDecimal("64000"), null).get(0).id();
        UUID reglementId = service.regler(tontine.getId(), id, new BigDecimal("64000"), null)
                .get(0).reglements().get(0).id();

        List<ArriereResponse> liste = service.annulerReglement(tontine.getId(), id, reglementId);

        assertThat(liste.get(0).resteDu()).isEqualByComparingTo("64000");
        assertThat(fonds.getSolde()).isEqualByComparingTo("1000");
    }

    @Test
    void supprimer_refuseSiDejaRegle() {
        UUID id = service.creer(tontine.getId(), rivier.getId(), null, new BigDecimal("64000"), null).get(0).id();
        service.regler(tontine.getId(), id, new BigDecimal("1000"), null);

        assertThatThrownBy(() -> service.supprimer(tontine.getId(), id)).isInstanceOf(IllegalStateException.class);
    }
}
