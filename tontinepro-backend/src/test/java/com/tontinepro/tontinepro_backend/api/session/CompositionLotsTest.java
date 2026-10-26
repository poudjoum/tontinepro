package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.session.CompositionLots.Mise;
import com.tontinepro.tontinepro_backend.api.session.CompositionLots.Part;
import com.tontinepro.tontinepro_backend.api.session.CompositionLots.Proposition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositionLotsTest {

    private static final BigDecimal LOT = new BigDecimal("50000");

    private static Mise mise(UUID id, String montant) {
        return new Mise(id, new BigDecimal(montant));
    }

    private static BigDecimal total(List<Part> lot) {
        return lot.stream().map(Part::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void combinaisonsExactesDesExemples() {
        // 10+10+30, 20+30, 25+25 : sept mises partielles → trois lots exacts
        List<Mise> mises = new ArrayList<>();
        for (String m : List.of("10000", "10000", "30000", "20000", "30000", "25000", "25000")) {
            mises.add(mise(UUID.randomUUID(), m));
        }
        for (long graine = 0; graine < 20; graine++) {
            Proposition p = CompositionLots.proposer(mises, LOT, new Random(graine));
            assertThat(p.complete()).isTrue();
            assertThat(p.lots()).hasSize(3);
            p.lots().forEach(l -> assertThat(total(l)).isEqualByComparingTo(LOT));
        }
    }

    @Test
    void neDepasseJamaisLeLot() {
        // 30 000 + 30 000 ne doit pas former un lot de 60 000
        Proposition p = CompositionLots.proposer(
                List.of(mise(UUID.randomUUID(), "30000"), mise(UUID.randomUUID(), "30000")), LOT, new Random(1));
        assertThat(p.lots()).isEmpty();
        assertThat(p.restes()).hasSize(2);
        assertThat(p.complete()).isFalse();
    }

    @Test
    void miseSuperieureAuLotDonnePlusieursTours() {
        UUID gros = UUID.randomUUID();
        Proposition p = CompositionLots.proposer(
                List.of(mise(gros, "110000"), mise(UUID.randomUUID(), "40000")), LOT, new Random(3));
        assertThat(p.complete()).isTrue();
        assertThat(p.lots()).hasSize(3);   // 50 + 50 + (10 + 40)
        long toursDuGros = p.lots().stream().filter(l -> l.stream().anyMatch(pt -> pt.membreId().equals(gros))).count();
        assertThat(toursDuGros).isEqualTo(3);
    }

    @Test
    void privilegieLesLotsAPeuDeMembres() {
        // 25+25 doit être préféré à 10+15+25 quand les deux sont possibles
        List<Mise> mises = List.of(mise(UUID.randomUUID(), "25000"), mise(UUID.randomUUID(), "25000"),
                mise(UUID.randomUUID(), "10000"), mise(UUID.randomUUID(), "15000"));
        Proposition p = CompositionLots.proposer(mises, LOT, new Random(7));
        assertThat(p.lots()).hasSize(1);
        assertThat(p.lots().get(0)).hasSize(2);
        assertThat(p.restes()).hasSize(2);
    }

    @Test
    void valideUneCompositionManuelle() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        List<Mise> mises = List.of(mise(a, "30000"), mise(b, "70000"));
        List<List<Part>> manuel = List.of(
                List.of(new Part(a, new BigDecimal("30000")), new Part(b, new BigDecimal("20000"))),
                List.of(new Part(b, new BigDecimal("50000"))));
        assertThat(CompositionLots.valider(manuel, mises, LOT)).hasSize(2);
    }

    @Test
    void refuseUnLotQuiNeFaitPasLeMontant() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        List<Mise> mises = List.of(mise(a, "30000"), mise(b, "30000"));
        assertThatThrownBy(() -> CompositionLots.valider(
                List.of(List.of(new Part(a, new BigDecimal("30000")), new Part(b, new BigDecimal("30000")))),
                mises, LOT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lot 1");
    }

    @Test
    void refuseUneMiseMalRepartie() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        List<Mise> mises = List.of(mise(a, "50000"), mise(b, "10000"));
        assertThatThrownBy(() -> CompositionLots.valider(
                List.of(List.of(new Part(a, new BigDecimal("50000")))), mises, LOT))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
