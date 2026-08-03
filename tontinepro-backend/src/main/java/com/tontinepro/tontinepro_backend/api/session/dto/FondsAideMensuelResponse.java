package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Reconstitution des fonds d'aide collectés mois par mois depuis le début de la session.
 * Matrice membres (lignes) × mois (colonnes) : chaque cellule = fond d'aide payé par le
 * membre pour ce mois, avec totaux par ligne, par colonne et total général.
 */
public record FondsAideMensuelResponse(

        UUID sessionId,
        int sessionNumero,
        String tontineNom,

        // Colonnes : les mois couverts par la session (du début à la fin / mois courant)
        List<MoisColonne> mois,

        // Lignes : un membre actif par ligne, cellules alignées sur `mois`
        List<LigneMembre> membres,

        BigDecimal totalGeneral

) {
    /** Colonne = un mois de la session avec le total collecté ce mois. */
    public record MoisColonne(int mois, int annee, BigDecimal total) {}

    /** Ligne = un membre ; `cellules` a la même taille et le même ordre que `mois`. */
    public record LigneMembre(
            UUID membreId,
            String matricule,
            String nomPrenom,
            String typeParticipation,   // TONTINE | AIDE_SOCIALE
            List<BigDecimal> cellules,
            BigDecimal total
    ) {}
}
