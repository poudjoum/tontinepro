package com.tontinepro.tontinepro_backend.api.auth;

import com.tontinepro.tontinepro_backend.api.auth.dto.TwoFaSetupResponse;
import com.tontinepro.tontinepro_backend.domain.user.User;
import com.tontinepro.tontinepro_backend.domain.user.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
public class TwoFaService {

    private static final String   TICKET_PREFIX = "2fa:ticket:";
    private static final Duration TICKET_TTL    = Duration.ofMinutes(5);
    private static final String   ISSUER        = "TontinePro";

    private final UserRepository      userRepository;
    private final StringRedisTemplate redisTemplate;
    private final SecretGenerator     secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator         qrGenerator     = new ZxingPngQrGenerator();
    private final CodeVerifier        codeVerifier;

    public TwoFaService(UserRepository userRepository, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.redisTemplate  = redisTemplate;
        TimeProvider  timeProvider  = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    // ── Setup ────────────────────────────────────────────────────────────

    /**
     * Génère un secret TOTP et le stocke sur l'utilisateur sans activer la 2FA.
     * Le membre doit confirmer avec un premier code valide ({@link #confirmer}).
     */
    @Transactional
    public TwoFaSetupResponse setup(String email) {
        User user = loadUser(email);

        if (user.isTwoFaEnabled()) {
            throw new IllegalStateException("La 2FA est déjà activée. Désactivez-la d'abord.");
        }

        String secret = secretGenerator.generate();
        user.setTwoFaSecret(secret);
        userRepository.save(user);

        QrData qrData = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer(ISSUER)
                .build();

        try {
            byte[] qrImage = qrGenerator.generate(qrData);
            String dataUri = "data:" + qrGenerator.getImageMimeType()
                    + ";base64," + Base64.getEncoder().encodeToString(qrImage);
            return new TwoFaSetupResponse(secret, dataUri);
        } catch (QrGenerationException e) {
            throw new RuntimeException("Erreur génération QR code 2FA", e);
        }
    }

    /**
     * Valide le premier code TOTP et active définitivement la 2FA.
     */
    @Transactional
    public void confirmer(String email, String code) {
        User user = loadUser(email);

        if (user.isTwoFaEnabled()) {
            throw new IllegalStateException("La 2FA est déjà activée.");
        }
        if (user.getTwoFaSecret() == null) {
            throw new IllegalStateException("Appelez d'abord /2fa/activer pour générer le secret.");
        }

        verifierCode(user.getTwoFaSecret(), code);

        user.setTwoFaEnabled(true);
        userRepository.save(user);
    }

    /**
     * Désactive la 2FA après vérification du code TOTP courant.
     */
    @Transactional
    public void desactiver(String email, String code) {
        User user = loadUser(email);

        if (!user.isTwoFaEnabled()) {
            throw new IllegalStateException("La 2FA n'est pas activée.");
        }

        verifierCode(user.getTwoFaSecret(), code);

        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        userRepository.save(user);
    }

    // ── Ticket (challenge éphémère) ──────────────────────────────────────

    /**
     * Crée un ticket Redis à durée de vie courte lié à l'userId.
     * Utilisé quand le login détecte que la 2FA est requise.
     */
    public String genererTicket(UUID userId) {
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(TICKET_PREFIX + ticket, userId.toString(), TICKET_TTL);
        return ticket;
    }

    /**
     * Consomme le ticket (suppression atomique) et retourne l'userId associé.
     * Lance une exception si le ticket est invalide ou expiré.
     */
    public UUID consommerTicket(String ticket) {
        String userId = redisTemplate.opsForValue().getAndDelete(TICKET_PREFIX + ticket);
        if (userId == null) {
            throw new IllegalArgumentException("Ticket 2FA invalide ou expiré");
        }
        return UUID.fromString(userId);
    }

    // ── Validation code ──────────────────────────────────────────────────

    public boolean estCodeValide(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void verifierCode(String secret, String code) {
        if (!codeVerifier.isValidCode(secret, code)) {
            throw new IllegalArgumentException("Code TOTP invalide ou expiré");
        }
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }
}
