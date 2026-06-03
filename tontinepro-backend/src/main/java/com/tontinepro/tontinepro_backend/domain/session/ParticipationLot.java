package com.tontinepro.tontinepro_backend.domain.session;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Inscription d'un membre à une session « à lot » avec sa mise mensuelle.
 * Renseignée pendant la période d'adhésion (avant figeage). Sert de base au
 * calcul des lots lors du figeage.
 */
@Entity
@Table(name = "participation_lot",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "membre_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private SessionTontine session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    /** Mise mensuelle du membre dans cette session (peut être < ou > au montant du lot). */
    @Column(name = "montant_mensuel", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantMensuel;

    @Column(name = "date_adhesion", nullable = false)
    @Builder.Default
    private LocalDate dateAdhesion = LocalDate.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
