package com.tontinepro.tontinepro_backend.api.rapport.builder;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tontinepro.tontinepro_backend.domain.aide.Aide;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class RapportMensuelPdfBuilder {

    private static final BaseColor BLEU_FONCE  = new BaseColor(44, 62, 80);
    private static final BaseColor BLEU_MOYEN  = new BaseColor(52, 152, 219);
    private static final BaseColor VERT        = new BaseColor(39, 174, 96);
    private static final BaseColor ROUGE       = new BaseColor(192, 57, 43);
    private static final BaseColor ORANGE      = new BaseColor(230, 126, 34);
    private static final BaseColor GRIS_CLAIR  = new BaseColor(245, 245, 245);

    private static final String[] MOIS_FR = {"", "Janvier", "Février", "Mars", "Avril", "Mai",
        "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] build(Tontine tontine, short mois, short annee,
                        List<Cotisation> cotisations,
                        List<Membre> tousLesMembres,
                        List<Aide> aidesDuMois) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36f, 36f, 50f, 36f);

        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            ajouterEntete(doc, tontine, mois, annee);
            doc.add(Chunk.NEWLINE);
            ajouterSectionCotisations(doc, cotisations, tousLesMembres, tontine);
            doc.add(Chunk.NEWLINE);
            ajouterSectionAides(doc, aidesDuMois);
            ajouterPiedDePage(doc);

        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF rapport mensuel", e);
        } finally {
            doc.close();
        }

        return baos.toByteArray();
    }

    // ── Entête ───────────────────────────────────────────────────────────

    private void ajouterEntete(Document doc, Tontine tontine, short mois, short annee)
            throws DocumentException {

        Font fTitre   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BLEU_FONCE);
        Font fSousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BLEU_MOYEN);
        Font fPetit   = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);

        Paragraph titre = new Paragraph("TONTINEPRO — Rapport Mensuel", fTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(4f);
        doc.add(titre);

        Paragraph periode = new Paragraph(tontine.getNom() + "   ·   "
                + MOIS_FR[mois] + " " + annee, fSousTitre);
        periode.setAlignment(Element.ALIGN_CENTER);
        periode.setSpacingAfter(4f);
        doc.add(periode);

        Paragraph genere = new Paragraph("Généré le : " + OffsetDateTime.now().format(FMT_DATE), fPetit);
        genere.setAlignment(Element.ALIGN_CENTER);
        genere.setSpacingAfter(10f);
        doc.add(genere);

        doc.add(separateur());
    }

    // ── Section Cotisations ──────────────────────────────────────────────

    private void ajouterSectionCotisations(Document doc, List<Cotisation> cotisations,
                                            List<Membre> membres, Tontine tontine)
            throws DocumentException {

        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_FONCE);
        doc.add(titreSectionParagraphe("COTISATIONS", fSection));

        // Stats
        long nbTotal    = membres.size();
        long nbPayees   = cotisations.stream().filter(c -> c.getStatut() == Cotisation.Statut.PAYEE).count();
        long nbRetard   = cotisations.stream().filter(c -> c.getStatut() == Cotisation.Statut.EN_RETARD).count();
        long nbAttente  = nbTotal - nbPayees - nbRetard;
        BigDecimal attendu  = tontine.getMontantCotisation().multiply(BigDecimal.valueOf(nbTotal));
        BigDecimal collecte = cotisations.stream()
                .filter(c -> c.getStatut() == Cotisation.Statut.PAYEE)
                .map(Cotisation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taux = nbTotal == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(nbPayees * 100L).divide(BigDecimal.valueOf(nbTotal), 1, RoundingMode.HALF_UP);

        doc.add(tableauStats(attendu, collecte, taux, nbPayees, nbRetard, nbAttente));
        doc.add(Chunk.NEWLINE);

        // Tableau détail
        if (!cotisations.isEmpty()) {
            doc.add(tableauCotisations(cotisations));
        } else {
            Font fInfo = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY);
            doc.add(new Paragraph("Aucune cotisation enregistrée pour cette période.", fInfo));
        }
    }

    private PdfPTable tableauStats(BigDecimal attendu, BigDecimal collecte, BigDecimal taux,
                                    long payees, long retard, long attente) throws DocumentException {
        Font fLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
        Font fValeur = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_FONCE);

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100f);
        t.setWidths(new float[]{33f, 33f, 34f});
        t.setSpacingAfter(8f);

        t.addCell(statCell("Total attendu", fcfa(attendu), fLabel, fValeur, GRIS_CLAIR));
        t.addCell(statCell("Total collecté", fcfa(collecte), fLabel, fValeur, GRIS_CLAIR));
        t.addCell(statCell("Taux de recouvrement", taux + " %", fLabel, fValeur, GRIS_CLAIR));
        t.addCell(statCell("Payées", String.valueOf(payees), fLabel, fValeur, new BaseColor(232, 245, 233)));
        t.addCell(statCell("En retard", String.valueOf(retard), fLabel, fValeur, new BaseColor(253, 235, 236)));
        t.addCell(statCell("En attente", String.valueOf(attente), fLabel, fValeur, new BaseColor(255, 249, 230)));

        return t;
    }

    private PdfPCell statCell(String label, String valeur, Font fLabel, Font fValeur, BaseColor bg) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", fLabel));
        p.add(new Chunk(valeur, fValeur));

        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(bg);
        cell.setPadding(8f);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        return cell;
    }

    private PdfPTable tableauCotisations(List<Cotisation> cotisations) throws DocumentException {
        Font fEnt  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
        Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable t = new PdfPTable(6);
        t.setWidthPercentage(100f);
        t.setWidths(new float[]{12f, 16f, 16f, 15f, 16f, 25f});
        t.setHeaderRows(1);

        for (String h : new String[]{"Matricule", "Nom", "Prénom", "Statut", "Montant", "Date paiement"}) {
            t.addCell(cellEntete(h, fEnt, BLEU_FONCE));
        }

        boolean pair = false;
        for (Cotisation c : cotisations) {
            BaseColor bg = pair ? BaseColor.WHITE : GRIS_CLAIR;
            pair = !pair;
            BaseColor couleurStatut = switch (c.getStatut()) {
                case PAYEE -> VERT;
                case EN_RETARD -> ROUGE;
                default -> ORANGE;
            };
            Font fStatut = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, couleurStatut);

            t.addCell(cellDonnee(c.getMembre().getMatricule(), fData, bg));
            t.addCell(cellDonnee(c.getMembre().getNom(), fData, bg));
            t.addCell(cellDonnee(c.getMembre().getPrenom(), fData, bg));
            t.addCell(cellDonnee(c.getStatut().name(), fStatut, bg));
            t.addCell(cellDonnee(fcfa(c.getMontant()), fData, bg));
            t.addCell(cellDonnee(
                    c.getDatePaiement() != null ? c.getDatePaiement().format(FMT_DATE) : "—", fData, bg));
        }
        return t;
    }

    // ── Section Aides ────────────────────────────────────────────────────

    private void ajouterSectionAides(Document doc, List<Aide> aides) throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_FONCE);
        doc.add(titreSectionParagraphe("AIDES MUTUELLES DU MOIS", fSection));

        long soumises = aides.stream().filter(a -> a.getStatut() == Aide.Statut.SOUMISE).count();
        long validees = aides.stream().filter(a -> a.getStatut() == Aide.Statut.VALIDEE
                                                || a.getStatut() == Aide.Statut.PAYEE).count();
        long rejetees = aides.stream().filter(a -> a.getStatut() == Aide.Statut.REJETEE).count();
        BigDecimal montantAccorde = aides.stream()
                .filter(a -> a.getMontantAccorde() != null)
                .map(Aide::getMontantAccorde).reduce(BigDecimal.ZERO, BigDecimal::add);

        Font fLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
        Font fValeur = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_FONCE);

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100f);
        t.setSpacingAfter(8f);
        t.addCell(statCell("Soumises",     String.valueOf(soumises),  fLabel, fValeur, GRIS_CLAIR));
        t.addCell(statCell("Validées",     String.valueOf(validees),  fLabel, fValeur, new BaseColor(232, 245, 233)));
        t.addCell(statCell("Rejetées",     String.valueOf(rejetees),  fLabel, fValeur, new BaseColor(253, 235, 236)));
        t.addCell(statCell("Accordé",      fcfa(montantAccorde),      fLabel, fValeur, GRIS_CLAIR));
        doc.add(t);
    }

    // ── Pied de page ─────────────────────────────────────────────────────

    private void ajouterPiedDePage(Document doc) throws DocumentException {
        doc.add(separateur());
        Font fPied = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.GRAY);
        Paragraph pied = new Paragraph("Document confidentiel — généré automatiquement par TontinePro", fPied);
        pied.setAlignment(Element.ALIGN_CENTER);
        doc.add(pied);
    }

    // ── Utilitaires ──────────────────────────────────────────────────────

    private PdfPCell cellEntete(String texte, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell cellDonnee(String texte, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        return cell;
    }

    private Paragraph titreSectionParagraphe(String titre, Font font) {
        Paragraph p = new Paragraph(titre, font);
        p.setSpacingBefore(6f);
        p.setSpacingAfter(6f);
        return p;
    }

    private PdfPTable separateur() throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100f);
        t.setSpacingBefore(4f);
        t.setSpacingAfter(4f);
        PdfPCell c = new PdfPCell();
        c.setBorderWidthBottom(0.5f);
        c.setBorderColorBottom(new BaseColor(200, 200, 200));
        c.setBorderWidthTop(0);
        c.setBorderWidthLeft(0);
        c.setBorderWidthRight(0);
        c.setMinimumHeight(1f);
        t.addCell(c);
        return t;
    }

    private static String fcfa(BigDecimal montant) {
        if (montant == null) return "0 FCFA";
        return String.format("%,.0f FCFA", montant).replace(",", " ");
    }
}
