package com.tontinepro.tontinepro_backend.api.rapport.builder;

import com.tontinepro.tontinepro_backend.domain.sanction.Sanction;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class RapportSanctionsExcelBuilder {

    private static final byte[] C_ENTETE  = {(byte) 44, (byte) 62, (byte) 80};
    private static final byte[] C_TITRE   = {(byte) 192, (byte) 57, (byte) 43};
    private static final byte[] C_PAYEE   = {(byte) 39, (byte) 174, (byte) 96};
    private static final byte[] C_ALT     = {(byte) 245, (byte) 245, (byte) 245};

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] build(Tontine tontine, List<Sanction> sanctions) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            creerDetail(wb, tontine, sanctions);
            creerResume(wb, tontine, sanctions);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur génération Excel sanctions", e);
        }
    }

    private void creerDetail(XSSFWorkbook wb, Tontine tontine, List<Sanction> sanctions) {
        Sheet sheet = wb.createSheet("Sanctions");

        CellStyle st = styleTitre(wb);
        Row rt = sheet.createRow(0);
        rt.setHeight((short) 600);
        Cell ct = rt.createCell(0);
        ct.setCellValue("SANCTIONS — " + tontine.getNom().toUpperCase());
        ct.setCellStyle(st);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        String[] headers = {"Matricule", "Membre", "Type", "Montant (FCFA)", "Motif", "Statut", "Date"};
        CellStyle se = styleEntete(wb);
        Row re = sheet.createRow(2);
        re.setHeight((short) 450);
        for (int i = 0; i < headers.length; i++) {
            Cell c = re.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(se);
        }

        CellStyle sNormal = styleNormal(wb, false);
        CellStyle sAlt    = styleNormal(wb, true);
        CellStyle sPayee  = stylePayee(wb);
        CellStyle sMontant = styleMontant(wb, false);
        CellStyle sMontantAlt = styleMontant(wb, true);

        int row = 3;
        for (Sanction s : sanctions) {
            boolean alt = (row % 2 == 0);
            Row r = sheet.createRow(row++);
            r.setHeight((short) 400);

            cell(r, 0, s.getMembre().getMatricule(),        alt ? sAlt : sNormal);
            cell(r, 1, s.getMembre().getPrenom() + " " + s.getMembre().getNom(), alt ? sAlt : sNormal);
            cell(r, 2, labelType(s.getTypeSanction()),      alt ? sAlt : sNormal);
            cellNum(r, 3, s.getMontant(),                   alt ? sMontantAlt : sMontant);
            cell(r, 4, s.getMotif() != null ? s.getMotif() : "—", alt ? sAlt : sNormal);
            CellStyle statut = s.isPayee() ? sPayee : (alt ? sAlt : sNormal);
            cell(r, 5, s.isPayee() ? "Réglée" : "Non réglée", statut);
            cell(r, 6, s.getCreatedAt().format(FMT),        alt ? sAlt : sNormal);
        }

        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 6000);
        sheet.setColumnWidth(2, 6000);
        sheet.setColumnWidth(3, 4500);
        sheet.setColumnWidth(4, 8000);
        sheet.setColumnWidth(5, 3500);
        sheet.setColumnWidth(6, 3500);
    }

    private void creerResume(XSSFWorkbook wb, Tontine tontine, List<Sanction> sanctions) {
        Sheet sheet = wb.createSheet("Résumé");

        CellStyle st = styleTitre(wb);
        Row rt = sheet.createRow(0);
        rt.setHeight((short) 600);
        Cell ct = rt.createCell(0);
        ct.setCellValue("RÉSUMÉ SANCTIONS");
        ct.setCellStyle(st);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        long nbTotal    = sanctions.size();
        long nbReglees  = sanctions.stream().filter(Sanction::isPayee).count();
        long nbImpayees = nbTotal - nbReglees;
        BigDecimal totalDu  = sanctions.stream().filter(s -> !s.isPayee()).map(Sanction::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaye = sanctions.stream().filter(Sanction::isPayee).map(Sanction::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        CellStyle se = styleEntete(wb);
        CellStyle sn = styleNormal(wb, false);

        String[][] data = {
            {"Total sanctions", String.valueOf(nbTotal)},
            {"Non réglées", String.valueOf(nbImpayees)},
            {"Réglées", String.valueOf(nbReglees)},
            {"Montant total dû (FCFA)", totalDu.toPlainString()},
            {"Montant total payé (FCFA)", totalPaye.toPlainString()},
        };

        for (int i = 0; i < data.length; i++) {
            Row r = sheet.createRow(i + 2);
            Cell c0 = r.createCell(0); c0.setCellValue(data[i][0]); c0.setCellStyle(se);
            Cell c1 = r.createCell(1); c1.setCellValue(data[i][1]); c1.setCellStyle(sn);
        }

        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 4000);
    }

    private String labelType(Sanction.TypeSanction t) {
        return switch (t) {
            case RETARD_COTISATION -> "Retard cotisation";
            case ABSENCE_REUNION   -> "Absence réunion";
            default                -> "Autre";
        };
    }

    private void cell(Row r, int col, String val, CellStyle style) {
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }

    private void cellNum(Row r, int col, BigDecimal val, CellStyle style) {
        Cell c = r.createCell(col); c.setCellValue(val.doubleValue()); c.setCellStyle(style);
    }

    private CellStyle styleTitre(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(C_TITRE, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle styleEntete(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(C_ENTETE, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private CellStyle styleNormal(XSSFWorkbook wb, boolean alt) {
        CellStyle s = wb.createCellStyle();
        if (alt) {
            s.setFillForegroundColor(new XSSFColor(C_ALT, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        s.setWrapText(true);
        return s;
    }

    private CellStyle stylePayee(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setColor(new XSSFColor(C_PAYEE, null));
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }

    private CellStyle styleMontant(XSSFWorkbook wb, boolean alt) {
        CellStyle s = styleNormal(wb, alt);
        s.setAlignment(HorizontalAlignment.RIGHT);
        DataFormat fmt = wb.createDataFormat();
        s.setDataFormat(fmt.getFormat("#,##0"));
        return s;
    }
}
