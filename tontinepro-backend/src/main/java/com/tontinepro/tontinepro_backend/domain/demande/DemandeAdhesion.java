package com.tontinepro.tontinepro_backend.domain.demande;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import com.tontinepro.tontinepro_backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "demandes_adhesion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DemandeAdhesion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 150)
    private String prenom;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 20)
    private String telephone;

    @Column(columnDefinition = "TEXT")
    private String motivation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Statut statut = Statut.EN_ATTENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_participation", length = 20)
    @Builder.Default
    private Membre.TypeParticipation typeParticipation = Membre.TypeParticipation.TONTINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement_aide", length = 20)
    private Membre.ModePaiementAide modePaiementAide;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par")
    private User traitePar;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Statut {
        EN_ATTENTE, APPROUVEE, REJETEE
    }
}
