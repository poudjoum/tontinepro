package com.tontinepro.tontinepro_backend.api.invitation;

import com.tontinepro.tontinepro_backend.api.auth.dto.AuthResponse;
import com.tontinepro.tontinepro_backend.api.invitation.dto.GenererInvitationRequest;
import com.tontinepro.tontinepro_backend.api.invitation.dto.InvitationResponse;
import com.tontinepro.tontinepro_backend.api.invitation.dto.RejoindreRequest;
import com.tontinepro.tontinepro_backend.api.invitation.dto.StatutInvitationResponse;
import com.tontinepro.tontinepro_backend.api.notification.NotificationService;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargneRepository;
import com.tontinepro.tontinepro_backend.domain.invitation.InvitationToken;
import com.tontinepro.tontinepro_backend.domain.invitation.InvitationTokenRepository;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.membre.MembreRepository;
import com.tontinepro.tontinepro_backend.domain.notification.Notification;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.tontine.TontineRepository;
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
import java.util.List;
import java.util.UUID;

import com.tontinepro.tontinepro_backend.domain.user.RefreshToken;
import com.tontinepro.tontinepro_backend.domain.user.RefreshTokenRepository;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationTokenRepository invitationTokenRepository;
    private final TontineRepository tontineRepository;
    private final UserRepository userRepository;
    private final MembreRepository membreRepository;
    private final CompteEpargneRepository compteEpargneRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationService notificationService;

    @Transactional
    public InvitationResponse generer(GenererInvitationRequest request, String creePar) {
        Tontine tontine = tontineRepository.findById(request.tontineId())
                .orElseThrow(() -> new IllegalArgumentException("Tontine introuvable"));

        if (!tontine.isActif()) {
            throw new IllegalArgumentException("Impossible de générer une invitation pour une tontine inactive");
        }

        User userCreateur = userRepository.findByEmail(creePar)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        int duree = request.dureeHeures() != null ? request.dureeHeures() : 72;

        InvitationToken token = InvitationToken.builder()
                .tontine(tontine)
                .token(UUID.randomUUID().toString().replace("-", ""))
                .expiresAt(OffsetDateTime.now().plusHours(duree))
                .creePar(userCreateur)
                .build();

        return InvitationResponse.from(invitationTokenRepository.save(token));
    }

    @Transactional(readOnly = true)
    public StatutInvitationResponse statut(String tokenStr) {
        return invitationTokenRepository.findByToken(tokenStr)
                .map(t -> {
                    boolean valide = !t.isUtilise() && t.getExpiresAt().isAfter(OffsetDateTime.now());
                    return new StatutInvitationResponse(
                            valide,
                            t.getTontine().getId(),
                            t.getTontine().getNom(),
                            t.getTontine().getDescription(),
                            t.getExpiresAt()
                    );
                })
                .orElse(new StatutInvitationResponse(false, null, null, null, null));
    }

    @Transactional
    public AuthResponse rejoindre(String tokenStr, RejoindreRequest request) {
        InvitationToken invitationToken = invitationTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Lien d'invitation invalide"));

        if (invitationToken.isUtilise()) {
            throw new IllegalArgumentException("Ce lien d'invitation a déjà été utilisé");
        }
        if (invitationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Ce lien d'invitation a expiré");
        }
        Tontine tontine = invitationToken.getTontine();

        // Permettre à un utilisateur existant de rejoindre via invitation
        // (il peut déjà être membre d'une autre tontine)
        User user;
        if (userRepository.existsByEmail(request.email())) {
            // Compte trouvé par email — on le réutilise
            user = userRepository.findByEmail(request.email()).orElseThrow();
            if (membreRepository.existsByUserIdAndTontineId(user.getId(), tontine.getId())) {
                throw new IllegalArgumentException("Vous êtes déjà membre de cette tontine");
            }
        } else if (request.telephone() != null && !request.telephone().isBlank()
                && userRepository.findByTelephone(request.telephone()).isPresent()) {
            // Compte pré-créé par l'admin avec ce numéro de téléphone mais un email placeholder :
            // on met à jour les credentials avec ceux fournis par le membre lui-même
            user = userRepository.findByTelephone(request.telephone()).orElseThrow();
            if (membreRepository.existsByUserIdAndTontineId(user.getId(), tontine.getId())) {
                throw new IllegalArgumentException("Vous êtes déjà membre de cette tontine");
            }
            user.setEmail(request.email());
            user.setHashedPassword(passwordEncoder.encode(request.password()));
            userRepository.save(user);
        } else {
            user = userRepository.save(User.builder()
                    .email(request.email())
                    .hashedPassword(passwordEncoder.encode(request.password()))
                    .telephone(request.telephone())
                    .role(User.Role.MEMBRE)
                    .build());
        }
        String matricule = membreRepository.genererMatriculeUnique();
        Membre membre = Membre.builder()
                .user(user)
                .tontine(tontine)
                .nom(request.nom())
                .prenom(request.prenom())
                .matricule(matricule)
                .build();
        membre = membreRepository.save(membre);

        compteEpargneRepository.save(CompteEpargne.builder().membre(membre).build());

        invitationToken.setUtilise(true);
        invitationTokenRepository.save(invitationToken);

        notificationService.notifier(user, Notification.Type.BIENVENUE,
                "Bienvenue dans la tontine " + tontine.getNom(),
                "Votre adhésion a été enregistrée. Matricule : " + membre.getMatricule(),
                null, null);

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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> lister(UUID tontineId) {
        return invitationTokenRepository.findAll().stream()
                .filter(t -> tontineId == null || t.getTontine().getId().equals(tontineId))
                .map(InvitationResponse::from)
                .toList();
    }
}
