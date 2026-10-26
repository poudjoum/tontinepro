package com.tontinepro.tontinepro_backend.domain.session;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Abandon d'un membre en cours de session « à lot ». {@code solde} = cotisé − reçu :
 * positif, c'est un avoir gelé restitué en fin de session ; négatif, une dette
 * envers le groupe.
 */
@Entity
@Table(name = "abandon_lot",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "membre_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbandonLot {

    public enum Statut { AVOIR_GELE, AVOIR_RESTITUE, DETTE, DETTE_SOLDEE, SOLDE_NUL }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionTontine session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "date_abandon", nullable = false)
    private LocalDate dateAbandon;

    @Column(length = 500)
    private String motif;

    /** Nombre de mois réellement payés dans la session avant l'abandon. */
    @Column(name = "mois_cotises", nullable = false)
    private int moisCotises;

    @Column(name = "total_cotise", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCotise;

    /** Parts de cagnotte déjà versées au membre (brut, retenue du fond comprise). */
    @Column(name = "total_recu", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRecu;

    /** Cotisé − reçu. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal solde;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut;

    @Column(name = "date_restitution")
    private LocalDate dateRestitution;

    @Column(name = "montant_rembourse", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal montantRembourse = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    /** Dette restant à rembourser (0 si ce n'est pas une dette). */
    public BigDecimal resteARembourser() {
        if (solde.signum() >= 0) return BigDecimal.ZERO;
        BigDecimal reste = solde.negate().subtract(montantRembourse);
        return reste.signum() > 0 ? reste : BigDecimal.ZERO;
    }
}
