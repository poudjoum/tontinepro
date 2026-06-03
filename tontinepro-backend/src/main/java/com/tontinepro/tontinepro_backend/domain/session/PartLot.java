package com.tontinepro.tontinepro_backend.domain.session;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Part d'un membre dans un lot/tour (mode A_LOT). Un {@link OrdreBeneficiaire} (slot)
 * porte une part s'il est plein, ou plusieurs parts s'il est partagé par des membres
 * regroupés. {@code partCagnotte} = fraction de la cagnotte revenant au membre
 * (= sa mise mensuelle × nombre de tours).
 */
@Entity
@Table(name = "part_lot",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ordre_beneficiaire_id", "membre_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartLot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordre_beneficiaire_id", nullable = false)
    private OrdreBeneficiaire ordreBeneficiaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    /** Mise mensuelle du membre contribuant à ce lot. */
    @Column(name = "montant_mensuel", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantMensuel;

    /** Fraction de la cagnotte revenant à ce membre lorsque le lot bénéficie. */
    @Column(name = "part_cagnotte", precision = 15, scale = 2)
    private BigDecimal partCagnotte;
}
