package com.tontinepro.tontinepro_backend.domain.session;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tour de remboursement d'une session « à lot » : dernier tour, financé par les membres
 * choisis par le bureau, qui rend leurs avoirs gelés aux membres partis. L'écart entre
 * ce qui est collecté et les avoirs rendus est porté par la trésorerie.
 */
@Entity
@Table(name = "tour_restitution",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourRestitution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionTontine session;

    @Column(name = "date_tour", nullable = false)
    private LocalDate dateTour;

    /** Clos : les avoirs gelés ont été rendus. */
    @Column(nullable = false)
    @Builder.Default
    private boolean clos = false;

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    /** Encaissé auprès des cotisants, figé à la clôture. */
    @Column(name = "montant_collecte", precision = 15, scale = 2)
    private BigDecimal montantCollecte;

    /** Avoirs gelés rendus, figés à la clôture. */
    @Column(name = "montant_restitue", precision = 15, scale = 2)
    private BigDecimal montantRestitue;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
