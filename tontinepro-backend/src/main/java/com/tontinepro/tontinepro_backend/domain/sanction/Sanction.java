package com.tontinepro.tontinepro_backend.domain.sanction;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sanctions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sanction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tontine_id", nullable = false)
    private Tontine tontine;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_sanction", nullable = false, length = 30)
    private TypeSanction typeSanction;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(nullable = false)
    @Builder.Default
    private boolean payee = false;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum TypeSanction {
        RETARD_COTISATION,
        ABSENCE_REUNION,
        RETARD_REUNION_T1,
        RETARD_REUNION_T2,
        RETARD_REUNION_T3,
        ECHEC_TONTINE_AVANT,
        ECHEC_TONTINE_APRES,
        TROUBLE_BAGARRE,
        TROUBLE_ENGUEULADE,
        TROUBLE_INSULTE,
        AUTRE
    }
}
