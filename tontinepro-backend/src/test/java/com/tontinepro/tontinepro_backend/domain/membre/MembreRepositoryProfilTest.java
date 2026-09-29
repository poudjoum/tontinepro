package com.tontinepro.tontinepro_backend.domain.membre;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Résolution du profil d'un compte présent dans une ou plusieurs tontines. */
class MembreRepositoryProfilTest {

    private static final String EMAIL = "membre@exemple.com";
    private final MembreRepository repo = mock(MembreRepository.class, CALLS_REAL_METHODS);

    @Test
    void laTontinePreciseeDesigneLeProfil() {
        UUID b = UUID.randomUUID();
        Membre dansB = Membre.builder().build();
        when(repo.findByUserEmailAndTontineId(EMAIL, b)).thenReturn(Optional.of(dansB));
        assertThat(repo.profil(EMAIL, b)).containsSame(dansB);
    }

    @Test
    void sansTontineUnProfilUniqueSuffit() {
        Membre seul = Membre.builder().build();
        when(repo.findAllByUserEmail(EMAIL)).thenReturn(List.of(seul));
        assertThat(repo.profil(EMAIL, null)).containsSame(seul);
    }

    @Test
    void sansTontinePlusieursProfilsSontRefuses() {
        when(repo.findAllByUserEmail(EMAIL))
                .thenReturn(List.of(Membre.builder().build(), Membre.builder().build()));
        assertThatThrownBy(() -> repo.profil(EMAIL, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plusieurs tontines");
    }
}
