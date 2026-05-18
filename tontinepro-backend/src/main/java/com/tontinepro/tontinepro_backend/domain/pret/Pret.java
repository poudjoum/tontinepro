package com.tontinepro.tontinepro_backend.domain.pret;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "prets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pret {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "montant_principal", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantPrincipal;

    @Column(name = "taux_interet", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxInteret;

    @Column(name = "duree_mois", nullable = false)
    private short dureeMois;

    @Column(name = "montant_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Statut statut = Statut.DEMANDE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private User validePar;

    @Column(name = "date_validation")
    private OffsetDateTime dateValidation;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @Column(name = "date_decaissement")
    private OffsetDateTime dateDecaissement;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Statut {
        DEMANDE, VALIDE, REJETE, EN_COURS, SOLDE
    }
}
