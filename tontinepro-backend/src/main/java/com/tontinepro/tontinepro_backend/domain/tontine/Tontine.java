package com.tontinepro.tontinepro_backend.domain.tontine;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "montant_cotisation_min", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantCotisationMin;

    @Column(name = "montant_cotisation_max", precision = 15, scale = 2)
    private BigDecimal montantCotisationMax;

    @Column(name = "montant_consensuel", precision = 15, scale = 2)
    private BigDecimal montantConsensuel;

    @Column(name = "jour_reference")
    private Integer jourReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_regle_periodicite", nullable = false, length = 40)
    @Builder.Default
    private TypeReglePeriodicite typeReglePeriodicite = TypeReglePeriodicite.MENSUEL_JOUR_FIXE;

    @Column(name = "date_prochaine_tontine")
    private LocalDate dateProchaineTontine;

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

    public enum TypeReglePeriodicite {
        HEBDOMADAIRE,
        MENSUEL_JOUR_FIXE,
        MENSUEL_DERNIER_SAMEDI,
        MENSUEL_DERNIER_DIMANCHE,
        MENSUEL_PREMIER_DIMANCHE_APRES,
        MENSUEL_DIMANCHE_SUR_OU_APRES,
        TRIMESTRIEL_JOUR_FIXE,
        SEMESTRIEL_JOUR_FIXE,
        ANNUEL_JOUR_FIXE,
        DATE_MANUELLE
    }
}
