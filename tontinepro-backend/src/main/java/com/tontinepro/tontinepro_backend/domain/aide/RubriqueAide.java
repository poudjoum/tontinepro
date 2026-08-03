package com.tontinepro.tontinepro_backend.domain.aide;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Barème d'aide d'une tontine : encode une rubrique du règlement intérieur
 * (ex. « Aide maladie »). Le montant de référence est interprété selon le mode
 * de calcul :
 *  - PAR_PERSONNE : montantReference = part par membre  → total = part × N
 *  - FORFAITAIRE  : montantReference = enveloppe totale  → part = total ÷ N
 * où N = nombre de membres actifs (bénéficiaire inclus).
 */
@Entity
@Table(name = "rubriques_aide")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubriqueAide {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @Column(nullable = false, length = 120)
    private String libelle;

    /** Catégorie (icône / regroupement). Peut valoir AUTRE pour une rubrique libre. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_aide", nullable = false, length = 50)
    @Builder.Default
    private Aide.TypeAide typeAide = Aide.TypeAide.AUTRE;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_calcul", nullable = false, length = 20)
    @Builder.Default
    private ModeCalcul modeCalcul = ModeCalcul.PAR_PERSONNE;

    @Column(name = "montant_reference", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantReference;

    /** L'aide peut être avancée par la trésorerie du fonds avant collecte. */
    @Column(nullable = false)
    @Builder.Default
    private boolean prefinancable = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum ModeCalcul {
        PAR_PERSONNE,   // montantReference = part par membre
        FORFAITAIRE     // montantReference = enveloppe totale à répartir
    }
}
