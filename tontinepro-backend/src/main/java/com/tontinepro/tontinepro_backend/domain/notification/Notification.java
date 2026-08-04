package com.tontinepro.tontinepro_backend.domain.notification;

import com.tontinepro.tontinepro_backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Type type;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private boolean lu = false;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum Type {
        // Aide
        AIDE_PROPOSEE, AIDE_SOUMISE, AIDE_VALIDEE, AIDE_REJETEE, AIDE_PAYEE,
        AIDE_RECOUVREMENT_RETARD,
        // Prêt
        PRET_DEMANDE, PRET_APPROUVE, PRET_REJETE, PRET_REMBOURSE, PRET_SOLDE,
        // Cotisation
        COTISATION_PAYEE, COTISATION_EN_RETARD,
        // Épargne
        EPARGNE_DEPOT, EPARGNE_RETRAIT, EPARGNE_INTERETS,
        // Auth
        BIENVENUE,
        // Adhésion
        DEMANDE_ADHESION,
        // Sanction
        SANCTION_INFLIGEE
    }
}
