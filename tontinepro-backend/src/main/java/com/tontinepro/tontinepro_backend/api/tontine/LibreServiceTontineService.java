package com.tontinepro.tontinepro_backend.api.tontine;

import com.tontinepro.tontinepro_backend.api.auth.dto.AuthResponse;
import com.tontinepro.tontinepro_backend.api.tontine.dto.CreerMaTontineRequest;
import com.tontinepro.tontinepro_backend.api.tontine.dto.CreerTontineAvecCompteRequest;
import com.tontinepro.tontinepro_backend.api.tontine.dto.TontineResponse;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAideRepository;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
import com.tontinepro.tontinepro_backend.domain.user.RefreshToken;
import com.tontinepro.tontinepro_backend.domain.user.RefreshTokenRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import com.tontinepro.tontinepro_backend.infrastructure.security.JwtProperties;
import com.tontinepro.tontinepro_backend.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LibreServiceTontineService {

    private final TontineRepository tontineRepository;
    private final UserRepository userRepository;
    private final MembreRepository membreRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final FondsAideRepository fondsAideRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;

    /**
     * Crée un compte + une tontine en une seule transaction (flux public sans compte existant).
     * Le créateur devient Secrétaire général avec le rôle SECRETAIRE.
     */
    @Transactional
    public AuthResponse creerAvecCompte(CreerTontineAvecCompteRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email");
        }
        if (tontineRepository.existsByNom(request.tontineNom())) {
            throw new IllegalArgumentException("Une tontine avec ce nom existe déjà");
        }

        // Créer le compte Secrétaire
        User user = userRepository.save(User.builder()
                .email(request.email())
                .hashedPassword(passwordEncoder.encode(request.password()))
                .telephone(request.telephone())
                .role(User.Role.SECRETAIRE)
                .build());

        // Créer la tontine
        Tontine tontine = tontineRepository.save(buildTontine(
                request.tontineNom(), request.tontineDescription(),
                request.montantCotisationMin(), request.typeAcces(), request.descriptionAcces()));

        fondsAideRepository.save(FondsAide.builder().tontine(tontine).build());

        // Créer le profil Secrétaire
        String matricule = membreRepository.genererMatriculeUnique();
        Membre membre = membreRepository.save(Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(request.nom())
                .prenom(request.prenom())
                .matricule(matricule)
                .fonction(Membre.Fonction.SECRETAIRE)
                .build());

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return buildAuthResponse(user);
    }

    /**
     * Crée une tontine pour un utilisateur déjà connecté sans profil membre.
     * Le créateur devient Secrétaire général.
     */
    @Transactional
    public TontineResponse creerMaTontine(String email, CreerMaTontineRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        // Note: pas de vérification d'appartenance ici car on crée une NOUVELLE tontine
        if (tontineRepository.existsByNom(request.nom())) {
            throw new IllegalArgumentException("Une tontine avec ce nom existe déjà");
        }

        // Promouvoir en Secrétaire
        user.setRole(User.Role.SECRETAIRE);
        userRepository.save(user);

        Tontine tontine = tontineRepository.save(buildTontine(
                request.nom(), request.description(),
                request.montantCotisationMin(), request.typeAcces(), request.descriptionAcces()));

        fondsAideRepository.save(FondsAide.builder().tontine(tontine).build());

        String matricule = membreRepository.genererMatriculeUnique();
        Membre membre = membreRepository.save(Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(request.membreNom())
                .prenom(request.membrePrenom())
                .matricule(matricule)
                .fonction(Membre.Fonction.SECRETAIRE)
                .build());

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        return TontineResponse.from(tontine);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Tontine buildTontine(String nom, String description, java.math.BigDecimal montantMin,
                                  Tontine.TypeAcces typeAcces, String descriptionAcces) {
        return Tontine.builder()
                .nom(nom)
                .description(description)
                .montantCotisationMin(montantMin)
                .typeAcces(typeAcces != null ? typeAcces : Tontine.TypeAcces.OUVERTE)
                .descriptionAcces(descriptionAcces)
                .build();
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String rawRefresh = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawRefresh))
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000))
                .build());
        return AuthResponse.of(accessToken, rawRefresh,
                jwtProperties.getAccessTokenExpirationMs(),
                user.getEmail(), user.getRole().name(), false, user.isMustChangePassword());
    }

    private static String sha256(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
