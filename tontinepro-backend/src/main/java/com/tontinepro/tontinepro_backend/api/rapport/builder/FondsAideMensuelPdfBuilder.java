package com.tontinepro.tontinepro_backend.api.rapport.builder;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tontinepro.tontinepro_backend.api.session.dto.FondsAideMensuelResponse;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * PDF paysage de la reconstitution des fonds d'aide collectés mois par mois :
 * matrice membres (lignes) × mois (colonnes) avec totaux par ligne, colonne et général.
 */
@Component
public class FondsAideMensuelPdfBuilder {

    private static final Color BLEU_FONCE  = new Color(44, 62, 80);
    private static final Color INDIGO      = new Color(63, 81, 181);
    private static final Color INDIGO_PALE = new Color(232, 234, 246);
    private static final Color GRIS_CLAIR  = new Color(245, 245, 245);
    private static final Color GRIS_TEXTE  = new Color(160, 160, 160);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] MOIS_COURT = {"", "Jan", "Fév", "Mar", "Avr", "Mai", "Juin",
            "Juil", "Août", "Sept", "Oct", "Nov", "Déc"};

    public byte[] build(FondsAideMensuelResponse r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30f, 30f, 46f, 30f);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();
            ajouterEntete(doc, r);
            doc.add(Chunk.NEWLINE);
            ajouterMatrice(doc, r);
            ajouterSituation(doc, r);
            ajouterPied(doc);
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF fonds d'aide", e);
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    private void ajouterEntete(Document doc, FondsAideMensuelResponse r) throws DocumentException {
        Font fTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, BLEU_FONCE);
        Font fSub   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, INDIGO);
        Font fPetit = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

        Paragraph titre = new Paragraph("FONDS D'AIDE COLLECTÉS — " + r.tontineNom(), fTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(4f);
        doc.add(titre);

        Paragraph p = new Paragraph(
                "Session n°" + r.sessionNumero()
                        + "   ·   Total collecté : " + fcfaUnite(r.totalGeneral()), fSub);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(4f);
        doc.add(p);

        Paragraph meta = new Paragraph(
                "Reconstitution mois par mois depuis le début de la session"
                        + "   ·   Montants en FCFA   ·   Généré le " + LocalDate.now().format(FMT), fPetit);
        meta.setAlignment(Element.ALIGN_CENTER);
        doc.add(meta);
    }

    private void ajouterMatrice(Document doc, FondsAideMensuelResponse r) throws DocumentException {
        int nbMois = r.mois().size();

        if (nbMois == 0 || r.membres().isEmpty()) {
            Font fVide = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY);
            Paragraph vide = new Paragraph(
                    "Aucune donnée de fond d'aide collectée sur cette session pour le moment.", fVide);
            vide.setAlignment(Element.ALIGN_CENTER);
            vide.setSpacingBefore(20f);
            doc.add(vide);
            return;
        }

        Font fEnt  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8, BLEU_FONCE);
        Font fVide = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS_TEXTE);
        Font fTotLigne = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, INDIGO);

        int nbCol = nbMois + 2; // Membre + mois + Total
        PdfPTable t = new PdfPTable(nbCol);
        t.setWidthPercentage(100f);
        t.setHeaderRows(1);
        t.setSpacingAfter(8f);

        // Largeurs : colonne membre plus large, total un peu large, mois égaux
        float[] widths = new float[nbCol];
        widths[0] = 26f;
        for (int i = 1; i <= nbMois; i++) widths[i] = 10f;
        widths[nbCol - 1] = 12f;
        try { t.setWidths(widths); } catch (DocumentException ignored) {}

        // En-tête
        t.addCell(cellEntete("Membre", fEnt, Element.ALIGN_LEFT));
        for (FondsAideMensuelResponse.MoisColonne c : r.mois()) {
            t.addCell(cellEntete(moisLabel(c.mois(), c.annee()), fEnt, Element.ALIGN_RIGHT));
        }
        t.addCell(cellEntete("Total", fEnt, Element.ALIGN_RIGHT));

        // Lignes membres
        boolean pair = false;
        for (FondsAideMensuelResponse.LigneMembre l : r.membres()) {
            Color bg = pair ? Color.WHITE : GRIS_CLAIR;
            pair = !pair;

            String nom = l.nomPrenom();
            if ("AIDE_SOCIALE".equals(l.typeParticipation())) nom += "  (AS)";
            t.addCell(cell(nom, fData, bg, Element.ALIGN_LEFT));

            for (BigDecimal v : l.cellules()) {
                boolean vide = v == null || v.signum() == 0;
                t.addCell(cell(vide ? "—" : montant(v), vide ? fVide : fData, bg, Element.ALIGN_RIGHT));
            }
            t.addCell(cell(montant(l.total()), fTotLigne, bg, Element.ALIGN_RIGHT));
        }

        // Ligne total / mois
        Font fTot = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        t.addCell(cell("TOTAL / mois", fTot, BLEU_FONCE, Element.ALIGN_LEFT));
        for (FondsAideMensuelResponse.MoisColonne c : r.mois()) {
            t.addCell(cell(montant(c.total()), fTot, BLEU_FONCE, Element.ALIGN_RIGHT));
        }
        t.addCell(cell(montant(r.totalGeneral()), fTot, INDIGO, Element.ALIGN_RIGHT));

        doc.add(t);
    }

    /**
     * Situation du fond par membre : versé, parts des aides versées sur le fonds
     * imputées au membre, solde et montant à rattraper en fin de session.
     */
    private void ajouterSituation(Document doc, FondsAideMensuelResponse r) throws DocumentException {
        if (r.membres().isEmpty()) return;

        boolean avecAnterieur = r.totalAnterieur() != null && r.totalAnterieur().signum() != 0;
        int nbAides = r.aides().size();

        Font fTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_FONCE);
        Font fNote  = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Paragraph titre = new Paragraph("SITUATION DU FOND PAR MEMBRE", fTitre);
        titre.setSpacingBefore(10f);
        doc.add(titre);
        Paragraph note = new Paragraph(
                "Fond prévu par membre : " + fcfaUnite(r.objectifFond())
                        + "   ·   Fond restant dû : part du fond prévu pas encore versée (au mois ou au bénéfice)."
                        + "   ·   À rattraper : parts des aides versées sur le fonds, à régler en fin de session.", fNote);
        note.setSpacingAfter(6f);
        doc.add(note);

        Font fEnt  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8, BLEU_FONCE);
        Font fVide = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS_TEXTE);
        Font fNeg  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(192, 57, 43));
        Font fRatt = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(180, 83, 9));

        int nbCol = 1 + 1 + (avecAnterieur ? 1 : 0) + nbAides + 3;
        PdfPTable t = new PdfPTable(nbCol);
        t.setWidthPercentage(100f);
        t.setHeaderRows(1);
        float[] widths = new float[nbCol];
        Arrays.fill(widths, 11f);
        widths[0] = 26f;
        try { t.setWidths(widths); } catch (DocumentException ignored) {}

        t.addCell(cellEntete("Membre", fEnt, Element.ALIGN_LEFT));
        t.addCell(cellEntete("Fond versé", fEnt, Element.ALIGN_RIGHT));
        if (avecAnterieur) t.addCell(cellEntete("Antérieur", fEnt, Element.ALIGN_RIGHT));
        for (FondsAideMensuelResponse.AideColonne a : r.aides()) {
            String date = a.datePaiement() != null ? "\n" + a.datePaiement().format(FMT) : "";
            t.addCell(cellEntete("− " + a.libelle() + date, fEnt, Element.ALIGN_RIGHT));
        }
        t.addCell(cellEntete("Solde", fEnt, Element.ALIGN_RIGHT));
        t.addCell(cellEntete("Fond restant dû", fEnt, Element.ALIGN_RIGHT));
        t.addCell(cellEntete("À rattraper (aides)", fEnt, Element.ALIGN_RIGHT));

        boolean pair = false;
        BigDecimal totalSolde = BigDecimal.ZERO;
        for (FondsAideMensuelResponse.LigneMembre l : r.membres()) {
            Color bg = pair ? Color.WHITE : GRIS_CLAIR;
            pair = !pair;
            String nom = l.nomPrenom();
            if ("AIDE_SOCIALE".equals(l.typeParticipation())) nom += "  (AS)";
            t.addCell(cell(nom, fData, bg, Element.ALIGN_LEFT));
            t.addCell(cell(montant(l.total()), fData, bg, Element.ALIGN_RIGHT));
            if (avecAnterieur) {
                boolean vide = l.anterieur().signum() == 0;
                t.addCell(cell(vide ? "—" : montant(l.anterieur()), vide ? fVide : fData, bg, Element.ALIGN_RIGHT));
            }
            for (BigDecimal p : l.partsAides()) {
                boolean vide = p.signum() == 0;
                t.addCell(cell(vide ? "—" : montant(p), vide ? fVide : fData, bg, Element.ALIGN_RIGHT));
            }
            t.addCell(cell(montant(l.solde()), l.solde().signum() < 0 ? fNeg : fData, bg, Element.ALIGN_RIGHT));
            boolean fondAJour = l.fondRestantDu().signum() == 0;
            t.addCell(cell(fondAJour ? "à jour" : montant(l.fondRestantDu()), fondAJour ? fVide : fData, bg, Element.ALIGN_RIGHT));
            boolean aJour = l.aRattraper().signum() == 0;
            t.addCell(cell(aJour ? "—" : montant(l.aRattraper()), aJour ? fVide : fRatt, bg, Element.ALIGN_RIGHT));
            totalSolde = totalSolde.add(l.solde());
        }

        Font fTot = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        t.addCell(cell("TOTAL", fTot, BLEU_FONCE, Element.ALIGN_LEFT));
        t.addCell(cell(montant(r.totalGeneral()), fTot, BLEU_FONCE, Element.ALIGN_RIGHT));
        if (avecAnterieur) t.addCell(cell(montant(r.totalAnterieur()), fTot, BLEU_FONCE, Element.ALIGN_RIGHT));
        for (FondsAideMensuelResponse.AideColonne a : r.aides()) {
            t.addCell(cell(montant(a.totalImpute()), fTot, BLEU_FONCE, Element.ALIGN_RIGHT));
        }
        t.addCell(cell(montant(totalSolde), fTot, BLEU_FONCE, Element.ALIGN_RIGHT));
        t.addCell(cell(montant(r.totalFondRestantDu()), fTot, BLEU_FONCE, Element.ALIGN_RIGHT));
        t.addCell(cell(montant(r.totalARattraper()), fTot, INDIGO, Element.ALIGN_RIGHT));

        doc.add(t);
    }

    private void ajouterPied(Document doc) throws DocumentException {
        Font fPied = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph pied = new Paragraph(
                "« — » = aucun fond collecté ce mois   ·   (AS) = contributeur Aide Sociale   ·   "
                        + "Document confidentiel généré par TontinePro", fPied);
        pied.setAlignment(Element.ALIGN_CENTER);
        pied.setSpacingBefore(6f);
        doc.add(pied);
    }

    private PdfPCell cellEntete(String texte, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(BLEU_FONCE);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private PdfPCell cell(String texte, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(align);
        cell.setBorderColor(new Color(225, 225, 225));
        return cell;
    }

    private String moisLabel(int mois, int annee) {
        String m = (mois >= 1 && mois <= 12) ? MOIS_COURT[mois] : String.valueOf(mois);
        return m + " " + String.format("%02d", annee % 100);
    }

    /** Montant compact sans unité (matrice) : 5000 -> "5 000". */
    private static String montant(BigDecimal m) {
        if (m == null) return "0";
        return String.format("%,.0f", m).replace(",", " ");
    }

    /** Montant avec unité (en-tête). */
    private static String fcfaUnite(BigDecimal m) {
        return montant(m) + " FCFA";
    }
}
