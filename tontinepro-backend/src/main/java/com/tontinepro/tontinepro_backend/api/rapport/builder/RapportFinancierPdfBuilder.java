package com.tontinepro.tontinepro_backend.api.rapport.builder;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tontinepro.tontinepro_backend.domain.aide.ContributionFondsAide;
import com.tontinepro.tontinepro_backend.domain.aide.FondsAide;
import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.epargne.CompteEpargne;
import com.tontinepro.tontinepro_backend.domain.pret.Pret;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RapportFinancierPdfBuilder {

    private static final BaseColor BLEU_FONCE = new BaseColor(44, 62, 80);
    private static final BaseColor BLEU_MOYEN = new BaseColor(52, 152, 219);
    private static final BaseColor GRIS_CLAIR = new BaseColor(245, 245, 245);
    private static final BaseColor VERT       = new BaseColor(39, 174, 96);

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] build(Tontine tontine,
                        List<CompteEpargne> comptes,
                        List<Pret> tousLesPrets,
                        FondsAide fondsAide,
                        List<ContributionFondsAide> contributionsAPayerFonds,
                        List<Cotisation> cotisationsAnnee) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36f, 36f, 50f, 36f);

        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            ajouterEntete(doc, tontine);
            doc.add(Chunk.NEWLINE);
            ajouterSectionEpargne(doc, comptes);
            doc.add(Chunk.NEWLINE);
            ajouterSectionPrets(doc, tousLesPrets);
            doc.add(Chunk.NEWLINE);
            ajouterSectionFondsAide(doc, fondsAide, contributionsAPayerFonds);
            doc.add(Chunk.NEWLINE);
            ajouterSectionCotisationsAnnee(doc, cotisationsAnnee, LocalDate.now().getYear());
            ajouterPiedDePage(doc);

        } catch (DocumentException e) {
            throw new RuntimeException("Erreur génération PDF rapport financier", e);
        } finally {
            doc.close();
        }

        return baos.toByteArray();
    }

    // ── Entête ───────────────────────────────────────────────────────────

    private void ajouterEntete(Document doc, Tontine tontine) throws DocumentException {
        Font fTitre  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BLEU_FONCE);
        Font fSub    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BLEU_MOYEN);
        Font fPetit  = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY);

        Paragraph p = new Paragraph("TONTINEPRO — Rapport Financier", fTitre);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(4f);
        doc.add(p);

        Paragraph sub = new Paragraph(tontine.getNom(), fSub);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(4f);
        doc.add(sub);

        Paragraph gen = new Paragraph("Généré le : " + OffsetDateTime.now().format(FMT_DATE), fPetit);
        gen.setAlignment(Element.ALIGN_CENTER);
        gen.setSpacingAfter(10f);
        doc.add(gen);

        doc.add(separateur());
    }

    // ── Section Épargne ──────────────────────────────────────────────────

    private void ajouterSectionEpargne(Document doc, List<CompteEpargne> comptes)
            throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_FONCE);
        doc.add(titreSection("ÉPARGNE", fSection));

        BigDecimal totalSolde = comptes.stream()
                .map(CompteEpargne::getSolde).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal soldeMoyen = comptes.isEmpty() ? BigDecimal.ZERO :
                totalSolde.divide(BigDecimal.valueOf(comptes.size()), 2, RoundingMode.HALF_UP);
        BigDecimal soldeMax = comptes.stream()
                .map(CompteEpargne::getSolde).max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        Font fLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
        Font fValeur = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_FONCE);

        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100f);
        t.setSpacingAfter(8f);
        t.addCell(statCell("Comptes actifs",   String.valueOf(comptes.size()), fLabel, fValeur));
        t.addCell(statCell("Solde total",      fcfa(totalSolde),               fLabel, fValeur));
        t.addCell(statCell("Solde moyen",      fcfa(soldeMoyen),               fLabel, fValeur));
        t.addCell(statCell("Solde le plus élevé", fcfa(soldeMax),              fLabel, fValeur));
        doc.add(t);

        // Top 5 épargnants
        List<CompteEpargne> top5 = comptes.stream()
                .filter(c -> c.getSolde().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(CompteEpargne::getSolde).reversed())
                .limit(5).toList();

        if (!top5.isEmpty()) {
            Font fTitreTbl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BLEU_FONCE);
            doc.add(new Paragraph("Top épargnants :", fTitreTbl));

            Font fEnt  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
            Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8);
            PdfPTable tbl = new PdfPTable(3);
            tbl.setWidthPercentage(60f);
            tbl.setWidths(new float[]{20f, 40f, 40f});
            tbl.setHorizontalAlignment(Element.ALIGN_LEFT);
            for (String h : new String[]{"Matricule", "Nom Prénom", "Solde"}) {
                tbl.addCell(cellEntete(h, fEnt, BLEU_MOYEN));
            }
            boolean pair = false;
            for (CompteEpargne c : top5) {
                BaseColor bg = pair ? BaseColor.WHITE : GRIS_CLAIR;
                pair = !pair;
                tbl.addCell(cellDonnee(c.getMembre().getMatricule(), fData, bg));
                tbl.addCell(cellDonnee(c.getMembre().getNom() + " " + c.getMembre().getPrenom(), fData, bg));
                tbl.addCell(cellDonnee(fcfa(c.getSolde()), fData, bg));
            }
            doc.add(tbl);
        }
    }

    // ── Section Prêts ────────────────────────────────────────────────────

    private void ajouterSectionPrets(Document doc, List<Pret> tousLesPrets) throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_FONCE);
        doc.add(titreSection("PRÊTS", fSection));

        long nbDemande  = tousLesPrets.stream().filter(p -> p.getStatut() == Pret.Statut.DEMANDE).count();
        long nbEnCours  = tousLesPrets.stream().filter(p -> p.getStatut() == Pret.Statut.EN_COURS).count();
        long nbSoldes   = tousLesPrets.stream().filter(p -> p.getStatut() == Pret.Statut.SOLDE).count();
        long nbRejetes  = tousLesPrets.stream().filter(p -> p.getStatut() == Pret.Statut.REJETE).count();

        List<Pret> pretsActifs = tousLesPrets.stream()
                .filter(p -> p.getStatut() == Pret.Statut.EN_COURS).toList();
        BigDecimal encours = pretsActifs.stream()
                .map(Pret::getMontantTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Font fLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
        Font fValeur = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_FONCE);

        PdfPTable stats = new PdfPTable(5);
        stats.setWidthPercentage(100f);
        stats.setSpacingAfter(8f);
        stats.addCell(statCell("En demande",  String.valueOf(nbDemande), fLabel, fValeur));
        stats.addCell(statCell("En cours",    String.valueOf(nbEnCours), fLabel, fValeur));
        stats.addCell(statCell("Soldés",      String.valueOf(nbSoldes),  fLabel, fValeur));
        stats.addCell(statCell("Rejetés",     String.valueOf(nbRejetes), fLabel, fValeur));
        stats.addCell(statCell("Encours total", fcfa(encours),           fLabel, fValeur));
        doc.add(stats);

        if (!pretsActifs.isEmpty()) {
            Font fTitreTbl = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BLEU_FONCE);
            doc.add(new Paragraph("Prêts en cours :", fTitreTbl));

            Font fEnt  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
            Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8);
            PdfPTable tbl = new PdfPTable(5);
            tbl.setWidthPercentage(100f);
            tbl.setWidths(new float[]{15f, 25f, 20f, 15f, 25f});
            for (String h : new String[]{"Matricule", "Nom Prénom", "Principal", "Taux (%)", "Montant total"}) {
                tbl.addCell(cellEntete(h, fEnt, BLEU_FONCE));
            }
            boolean pair = false;
            for (Pret p : pretsActifs) {
                BaseColor bg = pair ? BaseColor.WHITE : GRIS_CLAIR;
                pair = !pair;
                tbl.addCell(cellDonnee(p.getMembre().getMatricule(), fData, bg));
                tbl.addCell(cellDonnee(p.getMembre().getNom() + " " + p.getMembre().getPrenom(), fData, bg));
                tbl.addCell(cellDonnee(fcfa(p.getMontantPrincipal()), fData, bg));
                tbl.addCell(cellDonnee(p.getTauxInteret() + " %", fData, bg));
                tbl.addCell(cellDonnee(fcfa(p.getMontantTotal()), fData, bg));
            }
            doc.add(tbl);
        }
    }

    // ── Section Fonds d'Aide ─────────────────────────────────────────────

    private void ajouterSectionFondsAide(Document doc, FondsAide fondsAide,
                                          List<ContributionFondsAide> aRecevoir)
            throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_FONCE);
        doc.add(titreSection("FONDS D'AIDE", fSection));

        BigDecimal totalARecevoir = aRecevoir.stream()
                .map(ContributionFondsAide::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        Font fLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
        Font fValeur = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_FONCE);

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100f);
        t.setSpacingAfter(8f);
        t.addCell(statCell("Solde actuel",              fcfa(fondsAide.getSolde()),      fLabel, fValeur));
        t.addCell(statCell("Contributions à recevoir",  fcfa(totalARecevoir),            fLabel, fValeur));
        t.addCell(statCell("Mode de contribution",
                fondsAide.getTontine().getModeContributionAide().name(), fLabel, fValeur));
        doc.add(t);
    }

    // ── Section Cotisations année en cours ───────────────────────────────

    private void ajouterSectionCotisationsAnnee(Document doc, List<Cotisation> cotisations, int annee)
            throws DocumentException {
        Font fSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BLEU_FONCE);
        doc.add(titreSection("COTISATIONS — Année " + annee, fSection));

        BigDecimal totalAttendu = cotisations.stream()
                .map(Cotisation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCollecte = cotisations.stream()
                .filter(c -> c.getStatut() == Cotisation.Statut.PAYEE)
                .map(Cotisation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        long nbTotal  = cotisations.size();
        long nbPayees = cotisations.stream().filter(c -> c.getStatut() == Cotisation.Statut.PAYEE).count();
        BigDecimal taux = nbTotal == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(nbPayees * 100L).divide(BigDecimal.valueOf(nbTotal), 1, RoundingMode.HALF_UP);

        Font fLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);
        Font fValeur = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BLEU_FONCE);

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100f);
        t.setSpacingAfter(8f);
        t.addCell(statCell("Total attendu",    fcfa(totalAttendu),  fLabel, fValeur));
        t.addCell(statCell("Total collecté",   fcfa(totalCollecte), fLabel, fValeur));
        t.addCell(statCell("Taux global",      taux + " %",         fLabel, fValeur));
        doc.add(t);

        // Résumé par mois
        Map<Short, List<Cotisation>> parMois = cotisations.stream()
                .collect(Collectors.groupingBy(Cotisation::getMois));

        if (!parMois.isEmpty()) {
            Font fEnt  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BaseColor.WHITE);
            Font fData = FontFactory.getFont(FontFactory.HELVETICA, 8);

            PdfPTable tbl = new PdfPTable(5);
            tbl.setWidthPercentage(80f);
            tbl.setWidths(new float[]{20f, 25f, 25f, 15f, 15f});
            tbl.setHorizontalAlignment(Element.ALIGN_LEFT);
            for (String h : new String[]{"Mois", "Attendu", "Collecté", "Taux", "Payées/Total"}) {
                tbl.addCell(cellEntete(h, fEnt, BLEU_MOYEN));
            }

            String[] moisFr = {"", "Janv.", "Févr.", "Mars", "Avr.", "Mai", "Juin",
                    "Juil.", "Août", "Sept.", "Oct.", "Nov.", "Déc."};

            parMois.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> {
                        short m = e.getKey();
                        List<Cotisation> cots = e.getValue();
                        BigDecimal att = cots.stream().map(Cotisation::getMontant)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal col = cots.stream()
                                .filter(c -> c.getStatut() == Cotisation.Statut.PAYEE)
                                .map(Cotisation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
                        long p = cots.stream().filter(c -> c.getStatut() == Cotisation.Statut.PAYEE).count();
                        BigDecimal tx = cots.isEmpty() ? BigDecimal.ZERO :
                                BigDecimal.valueOf(p * 100L).divide(BigDecimal.valueOf(cots.size()),
                                        1, RoundingMode.HALF_UP);
                        boolean altRow = m % 2 == 0;
                        BaseColor bg = altRow ? GRIS_CLAIR : BaseColor.WHITE;
                        tbl.addCell(cellDonnee(m < moisFr.length ? moisFr[m] : String.valueOf(m), fData, bg));
                        tbl.addCell(cellDonnee(fcfa(att), fData, bg));
                        tbl.addCell(cellDonnee(fcfa(col), fData, bg));
                        tbl.addCell(cellDonnee(tx + " %", fData, bg));
                        tbl.addCell(cellDonnee(p + "/" + cots.size(), fData, bg));
                    });
            doc.add(tbl);
        }
    }

    private void ajouterPiedDePage(Document doc) throws DocumentException {
        doc.add(separateur());
        Font fPied = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, BaseColor.GRAY);
        Paragraph pied = new Paragraph("Document confidentiel — généré automatiquement par TontinePro", fPied);
        pied.setAlignment(Element.ALIGN_CENTER);
        doc.add(pied);
    }

    // ── Utilitaires ──────────────────────────────────────────────────────

    private PdfPCell statCell(String label, String valeur, Font fLabel, Font fValeur) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", fLabel));
        p.add(new Chunk(valeur, fValeur));
        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(GRIS_CLAIR);
        cell.setPadding(8f);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        return cell;
    }

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

    private Paragraph titreSection(String titre, Font font) {
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
        return String.format("%,.0f FCFA", montant).replace(",", " ");
    }
}
