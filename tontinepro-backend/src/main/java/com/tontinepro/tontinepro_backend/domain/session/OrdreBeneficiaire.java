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
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"session_id", "ordre"}),
           @UniqueConstraint(columnNames = {"session_id", "membre_id"})
       })
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

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
