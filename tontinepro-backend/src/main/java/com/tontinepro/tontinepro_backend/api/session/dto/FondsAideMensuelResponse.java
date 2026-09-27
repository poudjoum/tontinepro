package com.tontinepro.tontinepro_backend.api.session.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reconstitution des fonds d'aide collectés mois par mois depuis le début de la session.
 * Matrice membres (lignes) × mois (colonnes) : chaque cellule = fond d'aide payé par le
 * membre pour ce mois, avec totaux par ligne, par colonne et total général.
 *
 * <p>Les aides versées sur le fonds pendant la session sont imputées sur le fond de
 * chaque membre (sa part non encore remboursée) ; le membre rattrape en fin de session
 * de quoi retrouver l'objectif de fond prévu.</p>
 */
public record FondsAideMensuelResponse(

        UUID sessionId,
        int sessionNumero,
        String tontineNom,

        // Colonnes : les mois couverts par la session (du début à la fin / mois courant)
        List<MoisColonne> mois,

        // Lignes : un membre actif par ligne, cellules alignées sur `mois`
        List<LigneMembre> membres,

        BigDecimal totalGeneral,

        // Fond prévu par membre pour la session (fond annuel de la tontine)
        BigDecimal objectifFond,

        // Aides versées sur le fonds pendant la session, alignées sur LigneMembre.partsAides
        List<AideColonne> aides,

        BigDecimal totalAnterieur,
        BigDecimal totalImpute,
        BigDecimal totalFondRestantDu,
        BigDecimal totalARattraper

) {
    /** Colonne = un mois de la session avec le total collecté ce mois. */
    public record MoisColonne(int mois, int annee, BigDecimal total) {}

    /** Aide versée sur le fonds pendant la session. */
    public record AideColonne(
            UUID aideId,
            String libelle,
            String beneficiaire,
            LocalDate datePaiement,
            BigDecimal partParMembre,
            BigDecimal totalImpute
    ) {}

    /**
     * Ligne = un membre ; `cellules` a la même taille et le même ordre que `mois`,
     * `partsAides` la même taille et le même ordre que `aides`.
     */
    public record LigneMembre(
            UUID membreId,
            String matricule,
            String nomPrenom,
            String typeParticipation,   // TONTINE | AIDE_SOCIALE
            List<BigDecimal> cellules,
            BigDecimal total,
            // Fond versé avant l'application (reprise), compté dans le fond du membre
            BigDecimal anterieur,
            // Part de chaque aide imputée sur son fond (0 si déjà remboursée à la collecte)
            List<BigDecimal> partsAides,
            BigDecimal totalImpute,
            // Fond restant = versé (mois + antérieur) − parts imputées ; peut être négatif
            BigDecimal solde,
            // Fond prévu pas encore versé : max(0, objectif − versé − antérieur),
            // payé au fil des mois ou retenu au bénéfice
            BigDecimal fondRestantDu,
            // Parts d'aides imputées, à rattraper en fin de session
            BigDecimal aRattraper
    ) {}
}
