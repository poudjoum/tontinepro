package com.tontinepro.tontinepro_backend.domain.aide;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "aides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aide {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_aide", nullable = false, length = 50)
    private TypeAide typeAide;

    /** Rubrique du barème dont est issue la demande (null pour une aide libre). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubrique_id")
    private RubriqueAide rubrique;

    @Column(name = "montant_demande", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantDemande;

    @Column(name = "montant_accorde", precision = 15, scale = 2)
    private BigDecimal montantAccorde;

    // ── Snapshot d'activation (aides issues du barème) ────────────────────────
    /** Mode de calcul figé à l'activation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode_calcul", length = 20)
    private RubriqueAide.ModeCalcul modeCalcul;

    /** Montant de référence de la rubrique figé à l'activation. */
    @Column(name = "montant_reference", precision = 15, scale = 2)
    private BigDecimal montantReference;

    /** Nombre de membres actifs (N) au moment de l'activation. */
    @Column(name = "nb_membres_base")
    private Integer nbMembresBase;

    /** Part due par chaque membre. */
    @Column(name = "part_par_membre", precision = 15, scale = 2)
    private BigDecimal partParMembre;

    /** Vrai si l'aide a été avancée par la trésorerie avant collecte. */
    @Column(nullable = false)
    @Builder.Default
    private boolean prefinance = false;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Statut statut = Statut.SOUMISE;

    @Column(name = "justificatif_url", length = 500)
    private String justificatifUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par")
    private User validePar;

    @Column(name = "date_validation")
    private OffsetDateTime dateValidation;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public enum Statut {
        SOUMISE, VALIDEE, REJETEE, PAYEE
    }

    public enum TypeAide {
        DECES,        // Décès d'un membre ou d'un proche
        MALADIE,      // Hospitalisation ou frais médicaux importants
        ACCIDENT,     // Accident grave
        MARIAGE,      // Aide au mariage
        NAISSANCE,    // Naissance d'un enfant
        SCOLARITE,    // Frais de scolarité
        CALAMITE,     // Sinistre naturel (incendie, inondation…)
        AUTRE         // Toute autre situation exceptionnelle
    }
}
