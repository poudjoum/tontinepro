package com.tontinepro.tontinepro_backend.domain.aide;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Fonds non cotisé par un membre avant l'arrivée de la tontine dans l'application.
 * Ses règlements (voir {@link ReglementArriere}) créditent le fonds d'aide.
 */
@Entity
@Table(name = "arrieres_membres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArriereMembre {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_constat", nullable = false)
    private LocalDate dateConstat;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
