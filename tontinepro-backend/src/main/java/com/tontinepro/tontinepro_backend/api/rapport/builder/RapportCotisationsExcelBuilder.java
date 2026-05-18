package com.tontinepro.tontinepro_backend.api.rapport.builder;

import com.tontinepro.tontinepro_backend.domain.cotisation.Cotisation;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class RapportCotisationsExcelBuilder {

    private static final byte[] COULEUR_ENTETE  = {(byte) 44, (byte) 62, (byte) 80};
    private static final byte[] COULEUR_TITRE   = {(byte) 52, (byte) 152, (byte) 219};
    private static final byte[] COULEUR_PAYEE   = {(byte) 39, (byte) 174, (byte) 96};
    private static final byte[] COULEUR_RETARD  = {(byte) 192, (byte) 57, (byte) 43};
    private static final byte[] COULEUR_ALT     = {(byte) 245, (byte) 245, (byte) 245};

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] MOIS_FR = {"", "Janvier", "Février", "Mars", "Avril", "Mai",
        "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    public byte[] build(Tontine tontine, List<Cotisation> cotisations) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            creerFeuilleDetail(wb, tontine, cotisations);
            creerFeuilleResume(wb, tontine, cotisations);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur génération Excel cotisations", e);
        }
    }

    // ── Feuille Détail ───────────────────────────────────────────────────

    private void creerFeuilleDetail(XSSFWorkbook wb, Tontine tontine, List<Cotisation> cotisations) {
        Sheet sheet = wb.createSheet("Cotisations");

        // Titre fusionné
        CellStyle styleTitre = styleTitre(wb);
        Row rowTitre = sheet.createRow(0);
        rowTitre.setHeight((short) 600);
        Cell cellTitre = rowTitre.createCell(0);
        cellTitre.setCellValue("RAPPORT COTISATIONS — " + tontine.getNom().toUpperCase());
        cellTitre.setCellStyle(styleTitre);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

        // En-têtes
        CellStyle styleEntete = styleEntete(wb);
        String[] headers = {"N°", "Matricule", "Nom", "Prénom", "Mois", "Année",
                            "Montant (FCFA)", "Statut", "Date Paiement", "Référence"};
        Row rowHeader = sheet.createRow(1);
        rowHeader.setHeight((short) 450);
        for (int i = 0; i < headers.length; i++) {
            Cell c = rowHeader.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(styleEntete);
        }

        // Données
        CellStyle styleNormal = styleNormal(wb);
        CellStyle styleAlt    = styleAlt(wb);
        CellStyle stylePayee  = styleStatut(wb, COULEUR_PAYEE);
        CellStyle styleRetard = styleStatut(wb, COULEUR_RETARD);

        List<Cotisation> triees = cotisations.stream()
                .sorted(Comparator.comparingInt(Cotisation::getAnnee)
                        .thenComparingInt(Cotisation::getMois)
                        .thenComparing(c -> c.getMembre().getMatricule()))
                .toList();

        int rowIdx = 2;
        for (Cotisation cot : triees) {
            Row row = sheet.createRow(rowIdx);
            CellStyle cs = rowIdx % 2 == 0 ? styleAlt : styleNormal;

            row.createCell(0).setCellValue(rowIdx - 1);
            row.getCell(0).setCellStyle(cs);

            row.createCell(1).setCellValue(cot.getMembre().getMatricule());
            row.createCell(2).setCellValue(cot.getMembre().getNom());
            row.createCell(3).setCellValue(cot.getMembre().getPrenom());
            row.createCell(4).setCellValue(moisNom(cot.getMois()));
            row.createCell(5).setCellValue(cot.getAnnee());

            Cell cMontant = row.createCell(6);
            cMontant.setCellValue(cot.getMontant().doubleValue());

            Cell cStatut = row.createCell(7);
            cStatut.setCellValue(cot.getStatut().name());
            cStatut.setCellStyle(switch (cot.getStatut()) {
                case PAYEE -> stylePayee;
                case EN_RETARD -> styleRetard;
                default -> cs;
            });

            row.createCell(8).setCellValue(
                    cot.getDatePaiement() != null ? cot.getDatePaiement().format(FMT) : "");
            row.createCell(9).setCellValue(
                    cot.getReferencePaiement() != null ? cot.getReferencePaiement() : "");

            // Appliquer style aux cellules non-statut
            for (int col : new int[]{1, 2, 3, 4, 5, 6, 8, 9}) {
                if (row.getCell(col) != null) row.getCell(col).setCellStyle(cs);
            }

            rowIdx++;
        }

        // Largeurs automatiques
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 8000));
        }

        // Figer les deux premières lignes
        sheet.createFreezePane(0, 2);
        sheet.setAutoFilter(new CellRangeAddress(1, 1, 0, headers.length - 1));
    }

    // ── Feuille Résumé ───────────────────────────────────────────────────

    private void creerFeuilleResume(XSSFWorkbook wb, Tontine tontine, List<Cotisation> cotisations) {
        Sheet sheet = wb.createSheet("Résumé par période");

        CellStyle styleTitre  = styleTitre(wb);
        CellStyle styleEntete = styleEntete(wb);
        CellStyle styleNormal = styleNormal(wb);
        CellStyle styleAlt    = styleAlt(wb);

        Row rowTitre = sheet.createRow(0);
        rowTitre.setHeight((short) 600);
        Cell ct = rowTitre.createCell(0);
        ct.setCellValue("RÉSUMÉ PAR PÉRIODE — " + tontine.getNom().toUpperCase());
        ct.setCellStyle(styleTitre);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        String[] headers = {"Période", "Attendu (FCFA)", "Collecté (FCFA)", "Taux (%)",
                            "Payées", "En retard", "En attente"};
        Row rowH = sheet.createRow(1);
        rowH.setHeight((short) 450);
        for (int i = 0; i < headers.length; i++) {
            Cell c = rowH.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(styleEntete);
        }

        // Grouper par (annee, mois)
        Map<String, List<Cotisation>> grouped = cotisations.stream()
                .collect(Collectors.groupingBy(
                        c -> String.format("%04d-%02d", c.getAnnee(), c.getMois())));

        int rowIdx = 2;
        for (Map.Entry<String, List<Cotisation>> entry : new TreeMap<>(grouped).entrySet()) {
            List<Cotisation> group = entry.getValue();
            Cotisation premier = group.get(0);
            String periode = moisNom(premier.getMois()) + " " + premier.getAnnee();

            BigDecimal attendu  = group.stream().map(Cotisation::getMontant)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal collecte = group.stream()
                    .filter(c -> c.getStatut() == Cotisation.Statut.PAYEE)
                    .map(Cotisation::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
            long nbPayees  = group.stream().filter(c -> c.getStatut() == Cotisation.Statut.PAYEE).count();
            long nbRetard  = group.stream().filter(c -> c.getStatut() == Cotisation.Statut.EN_RETARD).count();
            long nbAttente = group.size() - nbPayees - nbRetard;
            BigDecimal taux = group.isEmpty() ? BigDecimal.ZERO :
                    BigDecimal.valueOf(nbPayees * 100L)
                            .divide(BigDecimal.valueOf(group.size()), 1, RoundingMode.HALF_UP);

            CellStyle cs = rowIdx % 2 == 0 ? styleAlt : styleNormal;
            Row row = sheet.createRow(rowIdx++);

            Cell c0 = row.createCell(0); c0.setCellValue(periode);       c0.setCellStyle(cs);
            Cell c1 = row.createCell(1); c1.setCellValue(attendu.doubleValue()); c1.setCellStyle(cs);
            Cell c2 = row.createCell(2); c2.setCellValue(collecte.doubleValue()); c2.setCellStyle(cs);
            Cell c3 = row.createCell(3); c3.setCellValue(taux.doubleValue()); c3.setCellStyle(cs);
            Cell c4 = row.createCell(4); c4.setCellValue(nbPayees);      c4.setCellStyle(cs);
            Cell c5 = row.createCell(5); c5.setCellValue(nbRetard);      c5.setCellStyle(cs);
            Cell c6 = row.createCell(6); c6.setCellValue(nbAttente);     c6.setCellStyle(cs);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 7000));
        }
        sheet.createFreezePane(0, 2);
    }

    // ── Styles ───────────────────────────────────────────────────────────

    private CellStyle styleTitre(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.WHITE.getIndex());
        cs.setFont(font);
        cs.setFillForegroundColor(new XSSFColor(COULEUR_TITRE, null));
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        return cs;
    }

    private CellStyle styleEntete(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.WHITE.getIndex());
        cs.setFont(font);
        cs.setFillForegroundColor(new XSSFColor(COULEUR_ENTETE, null));
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setBorderBottom(BorderStyle.THIN);
        cs.setBorderTop(BorderStyle.THIN);
        cs.setBorderLeft(BorderStyle.THIN);
        cs.setBorderRight(BorderStyle.THIN);
        return cs;
    }

    private CellStyle styleNormal(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        cs.setBorderBottom(BorderStyle.THIN);
        cs.setBorderLeft(BorderStyle.THIN);
        cs.setBorderRight(BorderStyle.THIN);
        cs.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return cs;
    }

    private CellStyle styleAlt(XSSFWorkbook wb) {
        CellStyle cs = styleNormal(wb);
        cs.setFillForegroundColor(new XSSFColor(COULEUR_ALT, null));
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cs;
    }

    private CellStyle styleStatut(XSSFWorkbook wb, byte[] couleur) {
        CellStyle cs = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        cs.setFont(font);
        cs.setFillForegroundColor(new XSSFColor(couleur, null));
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setAlignment(HorizontalAlignment.CENTER);
        cs.setVerticalAlignment(VerticalAlignment.CENTER);
        return cs;
    }

    private static String moisNom(short mois) {
        return mois >= 1 && mois <= 12 ? MOIS_FR[mois] : String.valueOf(mois);
    }
}
