package com.tontinepro.tontinepro_backend.domain.aide;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contributions_fonds_aide")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributionFondsAide {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fonds_aide_id", nullable = false)
    private FondsAide fondsAide;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    /** Mois de la période (1–12) — renseigné pour le mode MENSUEL. */
    @Column(name = "mois")
    private Short mois;

    /** Année de la période — renseignée pour le mode MENSUEL. */
    @Column(name = "annee")
    private Short annee;

    /** Aide à l'origine de la contribution — renseignée pour le mode A_LA_BENEFICIATION. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aide_id")
    private Aide aide;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Statut statut = Statut.A_PAYER;

    @Column(name = "date_paiement")
    private OffsetDateTime datePaiement;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Statut {
        A_PAYER, PAYEE
    }
}
