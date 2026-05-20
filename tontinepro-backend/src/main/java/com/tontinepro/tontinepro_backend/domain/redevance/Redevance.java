package com.tontinepro.tontinepro_backend.domain.redevance;

import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "redevances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Redevance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tontine_id", nullable = false, unique = true)
    private Tontine tontine;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montant = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Periodicite periodicite = Periodicite.MENSUEL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Statut statut = Statut.A_JOUR;

    @Column(name = "prochain_paiement")
    private LocalDate prochainPaiement;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Periodicite { MENSUEL, ANNUEL }
    public enum Statut { A_JOUR, EN_RETARD, GRATUIT }
}
