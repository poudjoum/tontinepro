package com.tontinepro.tontinepro_backend.api.setup;

import com.tontinepro.tontinepro_backend.api.auth.AuthService;
import com.tontinepro.tontinepro_backend.api.auth.dto.AuthResponse;
import com.tontinepro.tontinepro_backend.api.setup.dto.SetupRequest;
import com.tontinepro.tontinepro_backend.api.setup.dto.SetupStatusResponse;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SetupService {

    private final UserRepository          userRepository;
    private final TontineRepository       tontineRepository;
    private final MembreRepository        membreRepository;
    private final FondsAideRepository     fondsAideRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final PasswordEncoder         passwordEncoder;
    private final AuthService             authService;

    public SetupStatusResponse status() {
        return new SetupStatusResponse(userRepository.existsByRole(User.Role.ADMIN));
    }

    @Transactional
    public AuthResponse setup(SetupRequest req) {
        if (userRepository.existsByRole(User.Role.ADMIN)) {
            throw new IllegalStateException("La plateforme est déjà configurée.");
        }

        // ── Tontine ──────────────────────────────────────────────────────
        var mode = req.modeContributionAide() != null
                ? req.modeContributionAide()
                : Tontine.ModeContributionAide.AUCUN;

        Tontine tontine = tontineRepository.save(Tontine.builder()
                .nom(req.tontineNom())
                .description(req.tontineDescription())
                .montantCotisation(req.montantCotisation())
                .jourCotisation(req.jourCotisation())
                .tauxInteretPret(nvl(req.tauxInteretPret()))
                .tauxInteretEpargne(nvl(req.tauxInteretEpargne()))
                .modeContributionAide(mode)
                .montantCotisationAide(req.montantCotisationAide())
                .build());

        fondsAideRepository.save(FondsAide.builder().tontine(tontine).build());

        // ── Compte fondateur (ADMIN + Président) ─────────────────────────
        User user = userRepository.save(User.builder()
                .email(req.email())
                .hashedPassword(passwordEncoder.encode(req.password()))
                .telephone(req.telephone())
                .role(User.Role.ADMIN)
                .build());

        Membre membre = membreRepository.save(Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(req.nom())
                .prenom(req.prenom())
                .fonction(Membre.Fonction.PRESIDENT)
                .build());

        String matricule = "MBR-" + membre.getId().toString().replace("-", "").substring(0, 8).toUpperCase();
        membre.setMatricule(matricule);
        membreRepository.save(membre);

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return authService.loginDirectly(user);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
