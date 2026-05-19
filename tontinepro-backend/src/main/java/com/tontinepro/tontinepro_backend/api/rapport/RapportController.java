package com.tontinepro.tontinepro_backend.api.rapport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rapports")
@PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
@RequiredArgsConstructor
@Tag(name = "Rapports")
public class RapportController {

    private final RapportService rapportService;

    @GetMapping("/mensuel")
    @Operation(summary = "Rapport mensuel PDF (cotisations + aides du mois)")
    public ResponseEntity<byte[]> mensuel(
            @RequestParam UUID tontineId,
            @RequestParam short mois,
            @RequestParam short annee
    ) {
        byte[] pdf = rapportService.rapportMensuel(tontineId, mois, annee);
        return pdfResponse(pdf, "rapport-mensuel-%02d-%d.pdf".formatted(mois, annee));
    }

    @GetMapping("/cotisations")
    @Operation(summary = "Export Excel des cotisations (filtre optionnel par période)")
    public ResponseEntity<byte[]> cotisations(
            @RequestParam UUID tontineId,
            @RequestParam(required = false) Short mois,
            @RequestParam(required = false) Short annee
    ) {
        byte[] xlsx = rapportService.rapportCotisations(tontineId, mois, annee);
        String nom = mois != null && annee != null
                ? "cotisations-%02d-%d.xlsx".formatted(mois, annee)
                : annee != null
                    ? "cotisations-%d.xlsx".formatted(annee)
                    : "cotisations-complet.xlsx";
        return excelResponse(xlsx, nom);
    }

    @GetMapping("/financier")
    @Operation(summary = "Rapport financier PDF (épargne, prêts, fonds d'aide, cotisations année)")
    public ResponseEntity<byte[]> financier(@RequestParam UUID tontineId) {
        byte[] pdf = rapportService.rapportFinancier(tontineId);
        return pdfResponse(pdf, "rapport-financier.pdf");
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ResponseEntity<byte[]> pdfResponse(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(content.length);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> excelResponse(byte[] content, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(content.length);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
}
