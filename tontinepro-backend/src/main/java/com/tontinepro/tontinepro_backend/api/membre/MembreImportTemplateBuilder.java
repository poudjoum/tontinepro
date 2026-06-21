package com.tontinepro.tontinepro_backend.api.membre;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Génère le modèle Excel (.xlsx) d'importation des membres.
 * Trois feuilles : « Membres » (à remplir), « Instructions » et « Exemple ».
 *
 * <p>L'ordre et les libellés des colonnes de la feuille « Membres » doivent rester
 * cohérents avec le parsing de {@link MembreImportService}.
 */
@Component
public class MembreImportTemplateBuilder {

    /** Colonnes attendues, dans l'ordre. L'astérisque marque les champs obligatoires. */
    static final String[] COLONNES = {
            "Nom *", "Prénom *", "Email *", "Téléphone"
    };

    private static final byte[] C_ENTETE = {(byte) 44,  (byte) 62,  (byte) 80};   // bleu nuit
    private static final byte[] C_TITRE  = {(byte) 52,  (byte) 152, (byte) 219};  // bleu
    private static final byte[] C_OBLIG  = {(byte) 231, (byte) 76,  (byte) 60};   // rouge doux
    private static final byte[] C_NOTE   = {(byte) 253, (byte) 245, (byte) 230};  // crème
    private static final byte[] C_EXEMPLE = {(byte) 235, (byte) 245, (byte) 251}; // bleu très clair

    public byte[] build(String nomTontine) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            creerFeuilleMembres(wb, nomTontine);
            creerFeuilleInstructions(wb);
            creerFeuilleExemple(wb);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur génération du modèle d'import des membres", e);
        }
    }

    // ── Feuille « Membres » (à remplir) ──────────────────────────────────────
    private void creerFeuilleMembres(XSSFWorkbook wb, String nomTontine) {
        Sheet sheet = wb.createSheet("Membres");
        sheet.setDisplayGridlines(false);

        String titre = (nomTontine != null && !nomTontine.isBlank())
                ? "IMPORTATION DES MEMBRES — " + nomTontine.toUpperCase()
                : "IMPORTATION DES MEMBRES";

        Row rt = sheet.createRow(0);
        rt.setHeight((short) 700);
        Cell ct = rt.createCell(0);
        ct.setCellValue(titre);
        ct.setCellStyle(styleTitre(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COLONNES.length - 1));

        Row rNote = sheet.createRow(1);
        rNote.setHeight((short) 520);
        Cell cn = rNote.createCell(0);
        cn.setCellValue("Saisissez un membre par ligne sous les en-têtes. Les colonnes suivies de * sont obligatoires. "
                + "Ne modifiez pas l'ordre des colonnes. Voir la feuille « Instructions ».");
        cn.setCellStyle(styleNote(wb));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, COLONNES.length - 1));

        Row re = sheet.createRow(3);
        re.setHeight((short) 480);
        CellStyle se = styleEntete(wb);
        for (int i = 0; i < COLONNES.length; i++) {
            Cell c = re.createCell(i);
            c.setCellValue(COLONNES[i]);
            c.setCellStyle(se);
        }

        // Lignes vides pré-formatées prêtes à la saisie
        CellStyle sVide = styleCelluleVide(wb);
        for (int r = 4; r < 34; r++) {
            Row row = sheet.createRow(r);
            for (int i = 0; i < COLONNES.length; i++) {
                row.createCell(i).setCellStyle(sVide);
            }
        }

        int[] widths = {6000, 6000, 9000, 5000};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);
        sheet.createFreezePane(0, 4);
    }

    // ── Feuille « Instructions » ─────────────────────────────────────────────
    private void creerFeuilleInstructions(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet("Instructions");
        sheet.setDisplayGridlines(false);

        Row rt = sheet.createRow(0);
        rt.setHeight((short) 600);
        Cell ct = rt.createCell(0);
        ct.setCellValue("MODE D'EMPLOI");
        ct.setCellStyle(styleTitre(wb));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        Row re = sheet.createRow(2);
        CellStyle se = styleEntete(wb);
        Cell h0 = re.createCell(0); h0.setCellValue("Colonne"); h0.setCellStyle(se);
        Cell h1 = re.createCell(1); h1.setCellValue("Consigne"); h1.setCellStyle(se);

        String[][] lignes = {
            {"Nom *",       "Nom de famille du membre. Obligatoire."},
            {"Prénom *",    "Prénom du membre. Obligatoire."},
            {"Email *",     "Adresse email valide et unique. Sert d'identifiant de connexion. Obligatoire."},
            {"Téléphone",   "Numéro de téléphone (recommandé). Format libre, ex : 699001122 ou +237 699 00 11 22."},
            {"", ""},
            {"Après l'import", "Chaque nouveau membre reçoit un email avec son identifiant et un mot de passe"
                    + " temporaire qu'il devra obligatoirement changer à sa première connexion."},
            {"Doublons", "Si l'email ou le téléphone correspond à un compte déjà existant, ce compte est"
                    + " simplement rattaché à la tontine (aucun nouveau mot de passe n'est envoyé)."},
        };

        CellStyle sLabel  = styleLabel(wb);
        CellStyle sTexte  = styleTexteWrap(wb);
        int r = 3;
        for (String[] l : lignes) {
            Row row = sheet.createRow(r++);
            row.setHeight((short) 600);
            Cell c0 = row.createCell(0); c0.setCellValue(l[0]); c0.setCellStyle(sLabel);
            Cell c1 = row.createCell(1); c1.setCellValue(l[1]); c1.setCellStyle(sTexte);
        }

        sheet.setColumnWidth(0, 5500);
        sheet.setColumnWidth(1, 18000);
    }

    // ── Feuille « Exemple » ──────────────────────────────────────────────────
    private void creerFeuilleExemple(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet("Exemple");
        sheet.setDisplayGridlines(false);

        Row re = sheet.createRow(0);
        re.setHeight((short) 480);
        CellStyle se = styleEntete(wb);
        for (int i = 0; i < COLONNES.length; i++) {
            Cell c = re.createCell(i);
            c.setCellValue(COLONNES[i]);
            c.setCellStyle(se);
        }

        String[][] exemples = {
            {"Nkeng",   "Pauline", "pauline.nkeng@example.com", "699001122"},
            {"Mballa",  "Jean",    "jean.mballa@example.com",   "677334455"},
            {"Talla",   "Aïcha",   "aicha.talla@example.com",   "+237 690 22 33 44"},
        };

        CellStyle sEx = styleExemple(wb);
        int r = 1;
        for (String[] ex : exemples) {
            Row row = sheet.createRow(r++);
            row.setHeight((short) 380);
            for (int i = 0; i < ex.length; i++) {
                Cell c = row.createCell(i);
                c.setCellValue(ex[i]);
                c.setCellStyle(sEx);
            }
        }

        int[] widths = {6000, 6000, 9000, 5500};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);
    }

    // ── Styles ───────────────────────────────────────────────────────────────
    private CellStyle styleTitre(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 15);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(C_TITRE, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle styleNote(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true); f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(C_NOTE, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
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
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle styleCelluleVide(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.HAIR);
        s.setBorderLeft(BorderStyle.HAIR);
        s.setBorderRight(BorderStyle.HAIR);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle styleLabel(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.xssf.usermodel.XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setColor(new XSSFColor(C_OBLIG, null));
        s.setFont(f);
        s.setVerticalAlignment(VerticalAlignment.TOP);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }

    private CellStyle styleTexteWrap(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setWrapText(true);
        s.setVerticalAlignment(VerticalAlignment.TOP);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }

    private CellStyle styleExemple(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setItalic(true);
        s.setFont(f);
        s.setFillForegroundColor(new XSSFColor(C_EXEMPLE, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.HAIR);
        return s;
    }
}
