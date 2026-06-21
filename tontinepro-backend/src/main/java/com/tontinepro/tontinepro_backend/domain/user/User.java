package com.tontinepro.tontinepro_backend.domain.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 20, unique = true)
    private String telephone;

    @Column(name = "hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private Role role = Role.MEMBRE;

    @Column(name = "two_fa_enabled", nullable = false)
    @Builder.Default
    private boolean twoFaEnabled = false;

    @Column(name = "two_fa_secret", length = 255)
    private String twoFaSecret;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    /**
     * Vrai lorsqu'un mot de passe temporaire a été attribué (import de membres) :
     * l'utilisateur est forcé de le changer à sa première connexion.
     */
    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private boolean mustChangePassword = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Role {
        SUPER_ADMIN, // Opérateur plateforme — gère toutes les tontines
        ADMIN,       // Président — accès total
        SECRETAIRE,  // Secrétaire — gestion quotidienne
        MEMBRE,
        INVITE
    }
}
