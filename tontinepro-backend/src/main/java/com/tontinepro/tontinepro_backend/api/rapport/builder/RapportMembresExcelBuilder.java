package com.tontinepro.tontinepro_backend.api.rapport.builder;

import com.tontinepro.tontinepro_backend.domain.membre.Membre;
import com.tontinepro.tontinepro_backend.domain.tontine.Tontine;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RapportMembresExcelBuilder {

    private static final byte[] C_ENTETE = {(byte) 44,  (byte) 62,  (byte) 80};
    private static final byte[] C_TITRE  = {(byte) 52,  (byte) 152, (byte) 219};
    private static final byte[] C_VERT   = {(byte) 39,  (byte) 174, (byte) 96};
    private static final byte[] C_ROUGE  = {(byte) 192, (byte) 57,  (byte) 43};
    private static final byte[] C_ALT    = {(byte) 245, (byte) 245, (byte) 245};

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] build(Tontine tontine, List<Membre> membres) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            creerDetail(wb, tontine, membres);
            creerResume(wb, tontine, membres);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur génération Excel membres", e);
        }
    }

    private void creerDetail(XSSFWorkbook wb, Tontine tontine, List<Membre> membres) {
        Sheet sheet = wb.createSheet("Membres");

        CellStyle st = styleTitre(wb);
        Row rt = sheet.createRow(0);
        rt.setHeight((short) 600);
        Cell ct = rt.createCell(0);
        ct.setCellValue("RÉPERTOIRE MEMBRES — " + tontine.getNom().toUpperCase());
        ct.setCellStyle(st);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        String[] headers = {"Matricule", "Nom", "Prénom", "Email", "Téléphone",
                            "Fonction", "Statut", "Date adhésion"};
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
        CellStyle sActif  = styleStatut(wb, true);
        CellStyle sInactif = styleStatut(wb, false);

        int row = 3;
        for (Membre m : membres) {
            boolean alt = (row % 2 == 0);
            Row r = sheet.createRow(row++);
            r.setHeight((short) 400);

            CellStyle base = alt ? sAlt : sNormal;
            cell(r, 0, m.getMatricule(), base);
            cell(r, 1, m.getNom(), base);
            cell(r, 2, m.getPrenom(), base);
            cell(r, 3, m.getUser().getEmail(), base);
            cell(r, 4, m.getUser().getTelephone() != null ? m.getUser().getTelephone() : "—", base);
            cell(r, 5, labelFonction(m.getFonction()), base);
            CellStyle ss = m.getStatut() == Membre.Statut.ACTIF ? sActif : sInactif;
            cell(r, 6, labelStatut(m.getStatut()), ss);
            cell(r, 7, m.getDateAdhesion() != null ? m.getDateAdhesion().format(FMT_DATE) : "—", base);
        }

        int[] widths = {4000, 5000, 5000, 8000, 4500, 5000, 3500, 4000};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);
    }

    private void creerResume(XSSFWorkbook wb, Tontine tontine, List<Membre> membres) {
        Sheet sheet = wb.createSheet("Résumé");

        CellStyle st = styleTitre(wb);
        Row rt = sheet.createRow(0);
        rt.setHeight((short) 600);
        Cell ct = rt.createCell(0);
        ct.setCellValue("RÉSUMÉ MEMBRES");
        ct.setCellStyle(st);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        Map<Membre.Statut, Long> parStatut = membres.stream()
                .collect(Collectors.groupingBy(Membre::getStatut, Collectors.counting()));
        Map<Membre.Fonction, Long> parFonction = membres.stream()
                .collect(Collectors.groupingBy(Membre::getFonction, Collectors.counting()));

        CellStyle se = styleEntete(wb);
        CellStyle sn = styleNormal(wb, false);

        String[][] data = {
            {"Total membres", String.valueOf(membres.size())},
            {"Actifs", String.valueOf(parStatut.getOrDefault(Membre.Statut.ACTIF, 0L))},
            {"Suspendus", String.valueOf(parStatut.getOrDefault(Membre.Statut.SUSPENDU, 0L))},
            {"Retirés", String.valueOf(parStatut.getOrDefault(Membre.Statut.RETIRE, 0L))},
            {"", ""},
            {"Président", String.valueOf(parFonction.getOrDefault(Membre.Fonction.PRESIDENT, 0L))},
            {"Secrétaire", String.valueOf(parFonction.getOrDefault(Membre.Fonction.SECRETAIRE, 0L))},
            {"Trésorier", String.valueOf(parFonction.getOrDefault(Membre.Fonction.TRESORIER, 0L))},
            {"Censeur", String.valueOf(parFonction.getOrDefault(Membre.Fonction.CENSEUR, 0L))},
            {"Membres ordinaires", String.valueOf(parFonction.getOrDefault(Membre.Fonction.MEMBRE_ORDINAIRE, 0L))},
        };

        for (int i = 0; i < data.length; i++) {
            Row r = sheet.createRow(i + 2);
            Cell c0 = r.createCell(0); c0.setCellValue(data[i][0]); c0.setCellStyle(se);
            Cell c1 = r.createCell(1); c1.setCellValue(data[i][1]); c1.setCellStyle(sn);
        }

        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 4000);
    }

    private String labelStatut(Membre.Statut s) {
        return switch (s) {
            case ACTIF   -> "Actif";
            case SUSPENDU -> "Suspendu";
            case RETIRE  -> "Retiré";
        };
    }

    private String labelFonction(Membre.Fonction f) {
        return switch (f) {
            case PRESIDENT        -> "Président";
            case SECRETAIRE       -> "Secrétaire";
            case TRESORIER        -> "Trésorier";
            case CENSEUR          -> "Censeur";
            case MEMBRE_ORDINAIRE -> "Membre ordinaire";
        };
    }

    private void cell(Row r, int col, String val, CellStyle style) {
        Cell c = r.createCell(col); c.setCellValue(val != null ? val : ""); c.setCellStyle(style);
    }

    private CellStyle styleTitre(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.WHITE.getIndex());
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
        return s;
    }

    private CellStyle styleStatut(XSSFWorkbook wb, boolean actif) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f = (XSSFFont) wb.createFont();
        f.setBold(true);
        f.setColor(new XSSFColor(actif ? C_VERT : C_ROUGE, null));
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }
}
