package com.tontinepro.tontinepro_backend.domain.session;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions_tontine",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tontine_id", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionTontine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @Column(nullable = false)
    private int numero;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "date_prochaine_tontine")
    private LocalDate dateProchaineTontine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Statut statut = Statut.EN_COURS;

    @Column(name = "nombre_membres", nullable = false)
    private int nombreMembres;

    /** Objectif de membres visé avant démarrage de la session (optionnel). */
    @Column(name = "cible_membres")
    private Integer cibleMembres;

    @Column(name = "pot_reserve", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal potReserve = BigDecimal.ZERO;

    // ── Mode « tontine à lot » ────────────────────────────────────────────────

    /** Cagnotte (pot versé à chaque tour) figée au moment de la clôture des adhésions. */
    @Column(name = "cagnotte", precision = 15, scale = 2)
    private BigDecimal cagnotte;

    /** Poste de trésorerie dédié : surplus/reliquats pour payer les parts des membres partiels. */
    @Column(name = "tresorerie_lots", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tresorerieLots = BigDecimal.ZERO;

    /** Vrai une fois les adhésions closes et les lots constitués (mode A_LOT). */
    @Column(name = "figee", nullable = false)
    @Builder.Default
    private boolean figee = false;

    @Column(name = "date_figeage")
    private LocalDate dateFigeage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Statut {
        EN_COURS,
        TERMINEE,
        ANNULEE
    }
}
