package com.tontinepro.tontinepro_backend.domain.session;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ordre_beneficiaires",
       // Pas d'unicité (session, membre) : en mode à lot, un membre peut avoir plusieurs tours.
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "ordre"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdreBeneficiaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionTontine session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(nullable = false)
    private int ordre;

    @Column(name = "date_benefice")
    private LocalDate dateBenefice;

    @Column(name = "montant_recu", precision = 15, scale = 2)
    private BigDecimal montantRecu;

    @Column(nullable = false)
    @Builder.Default
    private boolean beneficie = false;

    /** Avance versée sur ce tour avant son échéance ; comprise dans {@code montantRecu} une fois payé. */
    @Column(name = "montant_avance", precision = 15, scale = 2)
    private BigDecimal montantAvance;

    @Column(name = "date_avance")
    private LocalDate dateAvance;

    // ── Détail des moyens de paiement du versement ──────────────────────────

    /** Montant remis en espèces au bénéficiaire. */
    @Column(name = "montant_cash", precision = 15, scale = 2)
    private BigDecimal montantCash;

    /** Montant versé par Orange Money au bénéficiaire. */
    @Column(name = "montant_om", precision = 15, scale = 2)
    private BigDecimal montantOm;

    /** Montant versé par MTN MoMo au bénéficiaire. */
    @Column(name = "montant_momo", precision = 15, scale = 2)
    private BigDecimal montantMomo;

    /** Référence de la transaction Orange Money. */
    @Column(name = "ref_om", length = 100)
    private String refOm;

    /** Référence de la transaction MTN MoMo. */
    @Column(name = "ref_momo", length = 100)
    private String refMomo;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
