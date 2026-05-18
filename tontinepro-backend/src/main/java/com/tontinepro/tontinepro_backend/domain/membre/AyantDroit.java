package com.tontinepro.tontinepro_backend.domain.membre;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ayants_droit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AyantDroit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(nullable = false, length = 150)
    private String prenom;

    @Column(name = "lien_parente", nullable = false, length = 50)
    private String lienParente;

    @Column(length = 20)
    private String telephone;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
