package com.tontinepro.tontinepro_backend.domain.tontine;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tontines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tontine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "montant_cotisation", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantCotisation;

    @Column(name = "jour_cotisation", nullable = false)
    @Builder.Default
    private short jourCotisation = 1;

    @Column(name = "taux_interet_pret", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxInteretPret = BigDecimal.ZERO;

    @Column(name = "taux_interet_epargne", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxInteretEpargne = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
