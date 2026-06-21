package com.tontinepro.tontinepro_backend.api.auth;

import com.tontinepro.tontinepro_backend.api.auth.dto.AuthResponse;
import com.tontinepro.tontinepro_backend.api.auth.dto.ClaimerCompteRequest;
import com.tontinepro.tontinepro_backend.api.auth.dto.LoginRequest;
import com.tontinepro.tontinepro_backend.api.auth.dto.RefreshRequest;
import com.tontinepro.tontinepro_backend.api.auth.dto.RegisterRequest;
import com.tontinepro.tontinepro_backend.api.auth.dto.ValiderTwoFaRequest;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.user.PasswordResetToken;
import com.tontinepro.tontinepro_backend.domain.user.PasswordResetTokenRepository;
import com.tontinepro.tontinepro_backend.domain.user.RefreshToken;
import com.tontinepro.tontinepro_backend.domain.user.RefreshTokenRepository;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import com.tontinepro.tontinepro_backend.infrastructure.security.JwtProperties;
import com.tontinepro.tontinepro_backend.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final TwoFaService twoFaService;
    private final NotificationService notificationService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email déjà utilisé : " + request.email());
        }

        User user = User.builder()
                .email(request.email())
                .hashedPassword(passwordEncoder.encode(request.password()))
                .telephone(request.telephone())
                .role(User.Role.MEMBRE)
                .build();

        userRepository.save(user);

        notificationService.notifier(user, Notification.Type.BIENVENUE,
                "Bienvenue sur TontinePro",
                "Votre compte a été créé avec succès. Bienvenue !",
                null, null);

        return buildAuthResponse(user);
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email()).orElseThrow();

        if (user.isTwoFaEnabled()) {
            String ticket = twoFaService.genererTicket(user.getId());
            return new LoginResult.TwoFaRequired(ticket, user.getEmail());
        }

        refreshTokenRepository.revokeAllByUserId(user.getId());
        return new LoginResult.Authenticated(buildAuthResponse(user));
    }

    @Transactional
    public AuthResponse loginWith2fa(ValiderTwoFaRequest request) {
        UUID userId = twoFaService.consommerTicket(request.ticket());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        if (!twoFaService.estCodeValide(user.getTwoFaSecret(), request.code())) {
            throw new IllegalArgumentException("Code TOTP invalide ou expiré");
        }

        refreshTokenRepository.revokeAllByUserId(user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = sha256(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token invalide"));

        if (stored.isRevoked()) {
            throw new IllegalArgumentException("Refresh token révoqué");
        }
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expiré");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        return buildAuthResponse(user);
    }

    /**
     * Permet à un membre pré-créé (par inscription directe admin) de réclamer son compte
     * en utilisant son numéro de téléphone comme identifiant fiable,
     * puis de définir son propre email et mot de passe.
     */
    @Transactional
    public AuthResponse activerCompte(ClaimerCompteRequest request) {
        User user = userRepository.findByTelephone(request.telephone())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Aucun compte trouvé pour ce numéro de téléphone"));

        // Si le nouvel email est différent, vérifier qu'il n'est pas déjà pris par un autre compte
        if (!request.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Cet email est déjà utilisé par un autre compte");
            }
            user.setEmail(request.email());
        }

        user.setHashedPassword(passwordEncoder.encode(request.motDePasse()));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        return buildAuthResponse(user);
    }

    /**
     * Change le mot de passe de l'utilisateur connecté et lève le drapeau
     * {@code mustChangePassword}. Utilisé lors de la première connexion d'un
     * membre importé qui dispose d'un mot de passe temporaire.
     */
    @Transactional
    public AuthResponse changerMotDePasse(String email, String nouveauMotDePasse) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        user.setHashedPassword(passwordEncoder.encode(nouveauMotDePasse));
        user.setMustChangePassword(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        return buildAuthResponse(user);
    }

    /** Connexion directe sans credentials — utilisé par SetupService après création initiale. */
    @Transactional
    public AuthResponse loginDirectly(User user) {
        refreshTokenRepository.revokeAllByUserId(user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String userEmail) {
        userRepository.findByEmail(userEmail)
                .ifPresent(user -> refreshTokenRepository.revokeAllByUserId(user.getId()));
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String rawRefreshToken = generateAndStoreRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                rawRefreshToken,
                jwtProperties.getAccessTokenExpirationMs(),
                user.getEmail(),
                user.getRole().name(),
                user.isTwoFaEnabled(),
                user.isMustChangePassword()
        );
    }

    private String generateAndStoreRefreshToken(User user) {
        String raw = UUID.randomUUID().toString();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(raw))
                .expiresAt(OffsetDateTime.now().plusSeconds(
                        jwtProperties.getRefreshTokenExpirationMs() / 1000))
                .build();

        refreshTokenRepository.save(token);
        return raw;
    }

    @Transactional(readOnly = true)
    public boolean verifierResetToken(String token) {
        return resetTokenRepository.findByToken(token)
                .map(t -> !t.isUsed() && !t.isExpire())
                .orElse(false);
    }

    @Transactional
    public void confirmerResetPassword(String token, String nouveauMotDePasse) {
        PasswordResetToken prt = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));

        if (prt.isUsed()) {
            throw new IllegalArgumentException("Ce lien a déjà été utilisé");
        }
        if (prt.isExpire()) {
            throw new IllegalArgumentException("Ce lien a expiré. Contactez votre administrateur.");
        }

        User user = prt.getUser();
        user.setHashedPassword(passwordEncoder.encode(nouveauMotDePasse));
        userRepository.save(user);

        prt.setUsed(true);
        resetTokenRepository.save(prt);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}
