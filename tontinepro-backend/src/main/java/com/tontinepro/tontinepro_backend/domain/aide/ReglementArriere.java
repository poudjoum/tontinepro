package com.tontinepro.tontinepro_backend.domain.aide;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Règlement (partiel ou total) d'un arriéré, encaissé par le bureau. */
@Entity
@Table(name = "reglements_arrieres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglementArriere {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arriere_id", nullable = false)
    private ArriereMembre arriere;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_reglement", nullable = false)
    private LocalDate dateReglement;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
