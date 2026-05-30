package com.tontinepro.tontinepro_backend.api.rapport.builder;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tontinepro.tontinepro_backend.api.session.dto.RapportTourResponse;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class RapportTourPdfBuilder {

    private static final Color BLEU_FONCE = new Color(44, 62, 80);
    private static final Color BLEU_MOYEN = new Color(52, 152, 219);
    private static final Color VERT       = new Color(39, 174, 96);
    private static final Color ROUGE      = new Color(192, 57, 43);
    private static final Color GRIS_CLAIR = new Color(245, 245, 245);
    private static final Color OR         = new Color(241, 196, 15);

    private static final String[] MOIS_FR = {"", "Janvier", "Février", "Mars", "Avril", "Mai",
        "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] build(RapportTourResponse r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36f, 36f, 50f, 36f);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();
            ajouterEntete(doc, r);
            doc.add(Chunk.NEWLINE);
            ajouterTableau(doc, r);
            doc.add(Chunk.NEWLINE);
            ajouterBilan(doc, r);
            ajouterPied(doc);
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF rapport de tour", e);
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    private void ajouterEntete(Document doc, RapportTourResponse r) throws DocumentException {
        Font fTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLEU_FONCE);
        Font fSub   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_MOYEN);
        Font fPetit = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

        Paragraph titre = new Paragraph("RAPPORT DE TOUR — " + r.tontineNom(), fTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(4f);
        doc.add(titre);

        String periodeStr = MOIS_FR[r.mois()] + " " + r.annee()
                + "   ·   Session n°" + r.sessionNumero();
        Paragraph periode = new Paragraph(periodeStr, fSub);
        periode.setAlignment(Element.ALIGN_CENTER);
        periode.setSpacingAfter(4f);
        doc.add(periode);

        String dateStr = r.dateTour() != null ? r.dateTour().format(FMT) : "—";
        Paragraph dateGen = new Paragraph("Date du tour : " + dateStr
                + "   ·   Généré le : " + LocalDate.now().format(FMT), fPetit);
        dateGen.setAlignment(Element.ALIGN_CENTER);
        dateGen.setSpacingAfter(8f);
        doc.add(dateGen);

        doc.add(separateur());

        // Bénéficiaire encadré
        Font fBen = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_FONCE);
        PdfPTable tBen = new PdfPTable(1);
        tBen.setWidthPercentage(100f);
        tBen.setSpacingAfter(8f);
        PdfPCell cBen = new PdfPCell(new Phrase(
                "BÉNÉFICIAIRE : " + r.beneficiaireNom() + "  (" + r.beneficiaireMatricule() + ")", fBen));
        cBen.setBackgroundColor(new Color(232, 240, 254));
        cBen.setPadding(8f);
        cBen.setBorderColor(BLEU_MOYEN);
        tBen.addCell(cBen);
        doc.add(tBen);
    }

    private void ajouterTableau(Document doc, RapportTourResponse r) throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_FONCE);
        Paragraph s = new Paragraph("COTISATIONS DES MEMBRES", fSection);
        s.setSpacingAfter(6f);
        doc.add(s);

        Font fEnt  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font fBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);

        PdfPTable t = new PdfPTable(6);
        t.setWidthPercentage(100f);
        t.setWidths(new float[]{30f, 14f, 14f, 14f, 14f, 6f});
        t.setHeaderRows(1);
        t.setSpacingAfter(8f);

        for (String h : new String[]{"Nom & Prénoms", "Cotisation", "Fond d'aide", "Repas", "Sanctions", "✓"}) {
            t.addCell(cellEntete(h, fEnt, BLEU_FONCE));
        }

        boolean pair = false;
        for (RapportTourResponse.LigneRapport l : r.lignes()) {
            Color bg = pair ? Color.WHITE : GRIS_CLAIR;
            pair = !pair;
            t.addCell(cellDonnee(l.nomPrenom(), fData, bg, Element.ALIGN_LEFT));
            t.addCell(cellDonnee(fcfa(l.cotisation()), fData, bg, Element.ALIGN_RIGHT));
            t.addCell(cellDonnee(fcfa(l.fond()), fData, bg, Element.ALIGN_RIGHT));
            t.addCell(cellDonnee(fcfa(l.repas()), fData, bg, Element.ALIGN_RIGHT));
            Color fSanct = l.sanctions().compareTo(BigDecimal.ZERO) > 0 ? ROUGE : Color.GRAY;
            t.addCell(cellDonnee(fcfa(l.sanctions()), FontFactory.getFont(FontFactory.HELVETICA, 8, fSanct), bg, Element.ALIGN_RIGHT));
            t.addCell(cellDonnee(l.paye() ? "✓" : "—",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, l.paye() ? VERT : Color.GRAY), bg, Element.ALIGN_CENTER));
        }

        // Ligne total
        Color bgTotal = new Color(44, 62, 80);
        Font fTot = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        t.addCell(cellDonnee("TOTAL", fTot, bgTotal, Element.ALIGN_LEFT));
        t.addCell(cellDonnee(fcfa(r.totalCotisations()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cellDonnee(fcfa(r.totalFond()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cellDonnee(fcfa(r.totalRepas()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cellDonnee(fcfa(r.totalSanctions()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cellDonnee("", fTot, bgTotal, Element.ALIGN_CENTER));

        doc.add(t);
    }

    private void ajouterBilan(Document doc, RapportTourResponse r) throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_FONCE);
        Paragraph s = new Paragraph("BILAN DU BÉNÉFICIAIRE", fSection);
        s.setSpacingAfter(6f);
        doc.add(s);

        Font fLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font fValeur = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_FONCE);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(80f);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.setWidths(new float[]{60f, 40f});
        t.setSpacingAfter(8f);

        lignesBilan(t, "Σ Cotisations collectées", fcfa(r.totalCotisations()), fLabel, fValeur, GRIS_CLAIR);
        lignesBilan(t, "Σ Repas collectés", fcfa(r.totalRepas()), fLabel, fValeur, GRIS_CLAIR);
        lignesBilan(t, "Pot brut (cotisations + repas)", fcfa(r.potBrut()), fLabel,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_MOYEN),
                new Color(232, 240, 254));
        if (r.totalFond().compareTo(BigDecimal.ZERO) > 0) {
            lignesBilan(t, "Σ Fond d'aide collecté → Trésorier", fcfa(r.totalFond()), fLabel,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(63, 81, 181)),
                    new Color(232, 234, 246));
        }

        if (r.fondAideObligation().compareTo(BigDecimal.ZERO) > 0) {
            lignesBilan(t, "Obligation fond d'aide annuelle", fcfa(r.fondAideObligation()), fLabel, fValeur, GRIS_CLAIR);
            lignesBilan(t, "Fond d'aide déjà versé cette année", fcfa(r.fondAidePayeAnnee()), fLabel, fValeur, GRIS_CLAIR);
            lignesBilan(t, "Déduction (dette fond d'aide)", "- " + fcfa(r.detteFondAide()), fLabel,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, ROUGE),
                    new Color(253, 235, 236));
        }

        // Cagnotte finale
        Font fCagnotte = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(39, 174, 96));
        PdfPCell lbl = new PdfPCell(new Phrase("CAGNOTTE NETTE DU BÉNÉFICIAIRE", fLabel));
        lbl.setBackgroundColor(new Color(232, 245, 233));
        lbl.setPadding(8f);
        lbl.setBorderColor(VERT);
        PdfPCell val = new PdfPCell(new Phrase(fcfa(r.cagnotteBeneficiaire()), fCagnotte));
        val.setBackgroundColor(new Color(232, 245, 233));
        val.setPadding(8f);
        val.setHorizontalAlignment(Element.ALIGN_RIGHT);
        val.setBorderColor(VERT);
        t.addCell(lbl);
        t.addCell(val);

        doc.add(t);
    }

    private void lignesBilan(PdfPTable t, String label, String valeur, Font fLabel, Font fValeur, Color bg) {
        PdfPCell lbl = new PdfPCell(new Phrase(label, fLabel));
        lbl.setBackgroundColor(bg);
        lbl.setPadding(6f);
        lbl.setBorderColor(new Color(220, 220, 220));
        PdfPCell val = new PdfPCell(new Phrase(valeur, fValeur));
        val.setBackgroundColor(bg);
        val.setPadding(6f);
        val.setHorizontalAlignment(Element.ALIGN_RIGHT);
        val.setBorderColor(new Color(220, 220, 220));
        t.addCell(lbl);
        t.addCell(val);
    }

    private void ajouterPied(Document doc) throws DocumentException {
        doc.add(separateur());
        Font fPied = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph pied = new Paragraph("Document confidentiel — généré automatiquement par TontinePro", fPied);
        pied.setAlignment(Element.ALIGN_CENTER);
        doc.add(pied);
    }

    private PdfPCell cellEntete(String texte, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell cellDonnee(String texte, Font font, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private PdfPTable separateur() throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100f);
        t.setSpacingBefore(4f);
        t.setSpacingAfter(4f);
        PdfPCell c = new PdfPCell();
        c.setBorderWidthBottom(0.5f);
        c.setBorderColorBottom(new Color(200, 200, 200));
        c.setBorderWidthTop(0);
        c.setBorderWidthLeft(0);
        c.setBorderWidthRight(0);
        c.setMinimumHeight(1f);
        t.addCell(c);
        return t;
    }

    private static String fcfa(BigDecimal m) {
        if (m == null) return "0 FCFA";
        return String.format("%,.0f FCFA", m).replace(",", " ");
    }
}
