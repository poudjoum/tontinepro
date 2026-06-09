package com.tontinepro.tontinepro_backend.api.rapport.builder;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tontinepro.tontinepro_backend.api.session.dto.RapportFinSessionResponse;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class RapportFinSessionPdfBuilder {

    private static final Color BLEU_FONCE = new Color(44, 62, 80);
    private static final Color BLEU_MOYEN = new Color(52, 152, 219);
    private static final Color VERT       = new Color(39, 174, 96);
    private static final Color GRIS_CLAIR = new Color(245, 245, 245);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] build(RapportFinSessionResponse r) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36f, 36f, 50f, 36f);
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();
            ajouterEntete(doc, r);
            doc.add(Chunk.NEWLINE);
            ajouterRecapitulatif(doc, r);
            doc.add(Chunk.NEWLINE);
            ajouterFiches(doc, r);
            ajouterPied(doc);
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF rapport de fin de session", e);
        } finally {
            doc.close();
        }
        return baos.toByteArray();
    }

    private void ajouterEntete(Document doc, RapportFinSessionResponse r) throws DocumentException {
        Font fTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLEU_FONCE);
        Font fSub   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_MOYEN);
        Font fPetit = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

        Paragraph titre = new Paragraph("RAPPORT DE FIN DE SESSION — " + r.tontineNom(), fTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(4f);
        doc.add(titre);

        String periode = "Du " + (r.dateDebut() != null ? r.dateDebut().format(FMT) : "—")
                + " au " + (r.dateFin() != null ? r.dateFin().format(FMT) : "—")
                + "   ·   Session n°" + r.sessionNumero();
        Paragraph p = new Paragraph(periode, fSub);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(4f);
        doc.add(p);

        String statutLabel = switch (r.statut()) {
            case "TERMINEE" -> "Session clôturée";
            case "EN_COURS" -> "Session en cours";
            default -> r.statut();
        };
        Paragraph meta = new Paragraph(statutLabel
                + "   ·   " + r.nbToursRealises() + "/" + r.nbToursTotal() + " tours réalisés"
                + "   ·   " + r.nombreMembres() + " membres"
                + "   ·   Généré le " + LocalDate.now().format(FMT), fPetit);
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(8f);
        doc.add(meta);

        doc.add(separateur());
    }

    private void ajouterRecapitulatif(Document doc, RapportFinSessionResponse r) throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_FONCE);
        Paragraph s = new Paragraph("RÉCAPITULATIF FINANCIER", fSection);
        s.setSpacingAfter(6f);
        doc.add(s);

        Font fLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font fValeur = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_FONCE);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(80f);
        t.setHorizontalAlignment(Element.ALIGN_CENTER);
        try { t.setWidths(new float[]{62f, 38f}); } catch (DocumentException ignored) {}
        t.setSpacingAfter(8f);

        ligne(t, "Σ Cotisations collectées (part tontine)", fcfa(r.totalCotisations()), fLabel, fValeur, GRIS_CLAIR);
        ligne(t, "Σ Repas collectés", fcfa(r.totalRepas()), fLabel, fValeur, GRIS_CLAIR);
        ligne(t, "Σ Fonds d'aide collecté", fcfa(r.totalFondAide()), fLabel,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(63, 81, 181)),
                new Color(232, 234, 246));
        ligne(t, "Σ Montants redistribués aux bénéficiaires", fcfa(r.totalRedistribue()), fLabel,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_MOYEN),
                new Color(232, 240, 254));
        ligne(t, "Solde du fonds de solidarité", fcfa(r.soldeFondsSolidarite()), fLabel, fValeur, GRIS_CLAIR);
        ligne(t, "Épargne totale des membres", fcfa(r.totalEpargne()), fLabel, fValeur, GRIS_CLAIR);
        ligne(t, "Prêts décaissés (principal)", fcfa(r.pretsTotalDecaisse()), fLabel, fValeur, GRIS_CLAIR);
        ligne(t, "Intérêts générés par les prêts (soldés)", fcfa(r.pretsInteretsGeneres()), fLabel,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, VERT), new Color(232, 245, 233));
        ligne(t, "Prêts en cours (montant restant)", fcfa(r.pretsEnCours()), fLabel, fValeur, GRIS_CLAIR);

        doc.add(t);
    }

    private void ajouterFiches(Document doc, RapportFinSessionResponse r) throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BLEU_FONCE);
        Paragraph s = new Paragraph("FICHES INDIVIDUELLES PAR MEMBRE", fSection);
        s.setSpacingAfter(6f);
        doc.add(s);

        Font fEnt  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable t = new PdfPTable(8);
        t.setWidthPercentage(100f);
        try {
            t.setWidths(new float[]{24f, 13f, 11f, 11f, 13f, 7f, 11f, 10f});
        } catch (DocumentException ignored) {}
        t.setHeaderRows(1);
        t.setSpacingAfter(8f);

        for (String h : new String[]{"Nom & Prénoms", "Cotisé", "Fond", "Repas", "Reçu", "Tour", "Épargne", "Prêt"}) {
            t.addCell(cellEntete(h, fEnt));
        }

        boolean pair = false;
        for (RapportFinSessionResponse.FicheMembre f : r.fiches()) {
            Color bg = pair ? Color.WHITE : GRIS_CLAIR;
            pair = !pair;
            t.addCell(cell(f.nomPrenom(), fData, bg, Element.ALIGN_LEFT));
            t.addCell(cell(fcfa(f.cotise()), fData, bg, Element.ALIGN_RIGHT));
            t.addCell(cell(fcfa(f.fondAideVerse()), fData, bg, Element.ALIGN_RIGHT));
            t.addCell(cell(fcfa(f.repasVerse()), fData, bg, Element.ALIGN_RIGHT));
            t.addCell(cell(f.aBeneficie() ? fcfa(f.recu()) : "—", fData, bg, Element.ALIGN_RIGHT));
            t.addCell(cell(f.aBeneficie() ? "✓" : "—",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, f.aBeneficie() ? VERT : Color.GRAY),
                    bg, Element.ALIGN_CENTER));
            t.addCell(cell(fcfa(f.epargne()), fData, bg, Element.ALIGN_RIGHT));
            t.addCell(cell(fcfa(f.pretEnCours()), fData, bg, Element.ALIGN_RIGHT));
        }

        // Ligne total
        Color bgTotal = BLEU_FONCE;
        Font fTot = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        t.addCell(cell("TOTAL", fTot, bgTotal, Element.ALIGN_LEFT));
        t.addCell(cell(fcfa(r.totalCotisations()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cell(fcfa(r.totalFondAide()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cell(fcfa(r.totalRepas()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cell(fcfa(r.totalRedistribue()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cell("", fTot, bgTotal, Element.ALIGN_CENTER));
        t.addCell(cell(fcfa(r.totalEpargne()), fTot, bgTotal, Element.ALIGN_RIGHT));
        t.addCell(cell(fcfa(r.pretsEnCours()), fTot, bgTotal, Element.ALIGN_RIGHT));

        doc.add(t);
    }

    private void ajouterPied(Document doc) throws DocumentException {
        doc.add(separateur());
        Font fPied = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        Paragraph pied = new Paragraph(
                "Document confidentiel — généré automatiquement par TontinePro", fPied);
        pied.setAlignment(Element.ALIGN_CENTER);
        doc.add(pied);
    }

    private void ligne(PdfPTable t, String label, String valeur, Font fLabel, Font fValeur, Color bg) {
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

    private PdfPCell cellEntete(String texte, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(BLEU_FONCE);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell cell(String texte, Font font, Color bg, int align) {
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
