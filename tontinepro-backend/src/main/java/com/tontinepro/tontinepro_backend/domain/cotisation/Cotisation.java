package com.tontinepro.tontinepro_backend.domain.cotisation;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "cotisations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_cotisation_periode",
        columnNames = {"membre_id", "mois", "annee"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cotisation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @Column(nullable = false)
    private short mois;

    @Column(nullable = false)
    private short annee;

    /** Part tontine (contribue au pot du bénéficiaire). */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    /** Part fonds d'aide (installment mensuel sur l'obligation annuelle). */
    @Column(name = "montant_fond_aide", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantFondAide = BigDecimal.ZERO;

    /** Contribution repas de la séance. */
    @Column(name = "montant_repas", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantRepas = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Statut statut = Statut.EN_ATTENTE;

    @Column(name = "date_paiement")
    private OffsetDateTime datePaiement;

    @Column(name = "reference_paiement", length = 100)
    private String referencePaiement;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Statut {
        EN_ATTENTE, PAYEE, EN_RETARD
    }
}
