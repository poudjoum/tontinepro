package com.tontinepro.tontinepro_backend.domain.membre;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "membres",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_membres_user_tontine",
        columnNames = {"user_id", "tontine_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membre {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @Column(nullable = false, unique = true, length = 50)
    private String matricule;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 150)
    private String prenom;

    @Column(name = "date_adhesion", nullable = false)
    @Builder.Default
    private LocalDate dateAdhesion = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Statut statut = Statut.ACTIF;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Fonction fonction = Fonction.MEMBRE_ORDINAIRE;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_participation", nullable = false, length = 20)
    @Builder.Default
    private TypeParticipation typeParticipation = TypeParticipation.TONTINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement_aide", length = 20)
    private ModePaiementAide modePaiementAide;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Statut {
        ACTIF, SUSPENDU, RETIRE
    }

    public enum Fonction {
        PRESIDENT,
        SECRETAIRE,
        TRESORIER,
        CENSEUR,
        MEMBRE_ORDINAIRE
    }

    public enum TypeParticipation {
        TONTINE,
        AIDE_SOCIALE
    }

    public enum ModePaiementAide {
        MENSUEL,
        EN_UNE_FOIS
    }
}
