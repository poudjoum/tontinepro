package com.tontinepro.tontinepro_backend.domain.tontine;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tontines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tontine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "montant_cotisation", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantCotisation;

    @Column(name = "jour_cotisation", nullable = false)
    @Builder.Default
    private short jourCotisation = 1;

    @Column(name = "taux_interet_pret", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxInteretPret = BigDecimal.ZERO;

    @Column(name = "taux_interet_epargne", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxInteretEpargne = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_contribution_aide", nullable = false, length = 30)
    @Builder.Default
    private ModeContributionAide modeContributionAide = ModeContributionAide.AUCUN;

    @Column(name = "montant_cotisation_aide", precision = 15, scale = 2)
    private BigDecimal montantCotisationAide;

    @Enumerated(EnumType.STRING)
    @Column(name = "periode_cotisation", nullable = false, length = 20)
    @Builder.Default
    private PeriodeCotisation periodeCotisation = PeriodeCotisation.MENSUEL;

    @Column(name = "montant_amende", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantAmende = BigDecimal.ZERO;

    @Column(name = "montant_penalite_retard", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantPenaliteRetard = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum ModeContributionAide {
        AUCUN,
        MENSUEL,
        A_LA_BENEFICIATION
    }

    public enum PeriodeCotisation {
        HEBDOMADAIRE,
        MENSUEL,
        BIMENSUEL,
        TRIMESTRIEL,
        SEMESTRIEL,
        ANNUEL
    }
}
