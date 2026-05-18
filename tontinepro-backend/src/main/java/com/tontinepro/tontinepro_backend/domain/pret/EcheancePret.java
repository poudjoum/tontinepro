package com.tontinepro.tontinepro_backend.domain.pret;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "echeances_pret",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_echeance_pret",
        columnNames = {"pret_id", "numero_echeance"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcheancePret {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pret_id", nullable = false)
    private Pret pret;

    @Column(name = "numero_echeance", nullable = false)
    private short numeroEcheance;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "montant_capital", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantCapital;

    @Column(name = "montant_interet", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantInteret;

    @Column(name = "montant_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Statut statut = Statut.A_PAYER;

    @Column(name = "date_paiement")
    private OffsetDateTime datePaiement;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public enum Statut {
        A_PAYER, PAYEE, EN_RETARD
    }
}
