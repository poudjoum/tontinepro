package com.tontinepro.tontinepro_backend.infrastructure.security;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cloisonnement des droits de gestion entre tontines. */
class SecurityExpressionServiceTest {

    private static final String SECRETAIRE_A = "secretaire@a";
    private static final String OPERATEUR = "operateur@plateforme";
    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    private final MembreRepository membres = mock(MembreRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TontineDeResolver tontineDe = mock(TontineDeResolver.class);
    private final SecurityExpressionService sec = new SecurityExpressionService(membres, users, tontineDe);

    @BeforeEach
    void donnees() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        when(membres.findByUserEmailAndTontineId(anyString(), any())).thenReturn(Optional.empty());

        // Secrétaire de A (rôle global SECRETAIRE), simple membre de B
        when(users.findByEmail(SECRETAIRE_A))
                .thenReturn(Optional.of(User.builder().email(SECRETAIRE_A).role(User.Role.SECRETAIRE).build()));
        when(membres.findByUserEmailAndTontineId(SECRETAIRE_A, A))
                .thenReturn(Optional.of(membre(Membre.Fonction.SECRETAIRE, Membre.Statut.ACTIF)));
        when(membres.findByUserEmailAndTontineId(SECRETAIRE_A, B))
                .thenReturn(Optional.of(membre(Membre.Fonction.MEMBRE_ORDINAIRE, Membre.Statut.ACTIF)));

        when(users.findByEmail(OPERATEUR))
                .thenReturn(Optional.of(User.builder().email(OPERATEUR).role(User.Role.SUPER_ADMIN).build()));
    }

    @Test
    void leSecretaireNeGereQueSaTontine() {
        assertThat(sec.gere(SECRETAIRE_A, A)).isTrue();
        assertThat(sec.gere(SECRETAIRE_A, B)).isFalse();
        assertThat(sec.gere(SECRETAIRE_A, UUID.randomUUID())).isFalse();
        assertThat(sec.gere(SECRETAIRE_A, null)).isFalse();
    }

    @Test
    void unDirigeantSuspenduNeGerePlus() {
        when(membres.findByUserEmailAndTontineId(SECRETAIRE_A, A))
                .thenReturn(Optional.of(membre(Membre.Fonction.SECRETAIRE, Membre.Statut.SUSPENDU)));
        assertThat(sec.gere(SECRETAIRE_A, A)).isFalse();
    }

    @Test
    void seulLePresidentPreside() {
        assertThat(sec.preside(SECRETAIRE_A, A)).isFalse();
        when(membres.findByUserEmailAndTontineId(SECRETAIRE_A, A))
                .thenReturn(Optional.of(membre(Membre.Fonction.PRESIDENT, Membre.Statut.ACTIF)));
        assertThat(sec.preside(SECRETAIRE_A, A)).isTrue();
        assertThat(sec.preside(SECRETAIRE_A, B)).isFalse();
    }

    @Test
    void leSuperAdminGereToutSansEtreMembre() {
        assertThat(sec.gere(OPERATEUR, A)).isTrue();
        assertThat(sec.preside(OPERATEUR, B)).isTrue();
        assertThat(sec.gereFiltre(OPERATEUR, null, null)).isTrue();
    }

    @Test
    void leCenseurNAccedeQueDansSaTontine() {
        when(membres.findByUserEmailAndTontineId(SECRETAIRE_A, B))
                .thenReturn(Optional.of(membre(Membre.Fonction.CENSEUR, Membre.Statut.ACTIF)));
        assertThat(sec.gereOuCenseur(SECRETAIRE_A, B)).isTrue();
        assertThat(sec.gere(SECRETAIRE_A, B)).isFalse();
    }

    @Test
    void uneListeSansFiltreEstReserveeAuSuperAdmin() {
        assertThat(sec.gereFiltre(SECRETAIRE_A, null, null)).isFalse();
        assertThat(sec.gereFiltre(SECRETAIRE_A, A, null)).isTrue();
        assertThat(sec.gereFiltre(SECRETAIRE_A, B, null)).isFalse();
    }

    @Test
    void unFiltreParMembreViseLaTontineDuMembre() {
        UUID membreDeB = UUID.randomUUID();
        when(tontineDe.membre(membreDeB)).thenReturn(B);
        assertThat(sec.gereFiltre(SECRETAIRE_A, null, membreDeB)).isFalse();
        // Associer sa propre tontine ne suffit pas à lire un membre d'ailleurs
        assertThat(sec.gereFiltre(SECRETAIRE_A, A, membreDeB)).isFalse();
    }

    @Test
    void lesLecturesSontOuvertesAuxSeulsMembresDeLaTontine() {
        String membreDeB = "membre@b";
        when(membres.findByUserEmailAndTontineId(membreDeB, B))
                .thenReturn(Optional.of(membre(Membre.Fonction.MEMBRE_ORDINAIRE, Membre.Statut.ACTIF)));
        assertThat(sec.membreOuGere(membreDeB, B)).isTrue();
        assertThat(sec.membreOuGere(membreDeB, A)).isFalse();
        assertThat(sec.membreOuGere(SECRETAIRE_A, A)).isTrue();
        assertThat(sec.membreOuGere(OPERATEUR, B)).isTrue();

        when(membres.findByUserEmailAndTontineId(membreDeB, B))
                .thenReturn(Optional.of(membre(Membre.Fonction.MEMBRE_ORDINAIRE, Membre.Statut.RETIRE)));
        assertThat(sec.membreOuGere(membreDeB, B)).isFalse();
    }

    @Test
    void seulUnCompteJamaisActivePeutEtreReclameParTelephone() {
        assertThat(User.builder().email("tel-690000000" + User.DOMAINE_EMAIL_TECHNIQUE).build()
                .enAttenteActivation()).isTrue();
        assertThat(User.builder().email("vrai@mail.com").mustChangePassword(true).build()
                .enAttenteActivation()).isTrue();
        assertThat(User.builder().email("vrai@mail.com").build()
                .enAttenteActivation()).isFalse();
        assertThat(User.builder().email("tel-1" + User.DOMAINE_EMAIL_TECHNIQUE)
                .role(User.Role.SUPER_ADMIN).mustChangePassword(true).build()
                .enAttenteActivation()).isFalse();
    }

    private static Membre membre(Membre.Fonction fonction, Membre.Statut statut) {
        return Membre.builder().fonction(fonction).statut(statut).build();
    }
}
