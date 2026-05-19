package com.tontinepro.tontinepro_backend.api.absence;

import com.tontinepro.tontinepro_backend.api.absence.dto.AbsenceResponse;
import com.tontinepro.tontinepro_backend.api.absence.dto.EnregistrerAbsenceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/absences")
@RequiredArgsConstructor
@Tag(name = "Absences")
public class AbsenceController {

    private final AbsenceService absenceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE') or @sec.isCenseur(authentication.name)")
    @Operation(summary = "Enregistrer une absence (Censeur / Secrétaire / Président)")
    public AbsenceResponse enregistrer(
            @Valid @RequestBody EnregistrerAbsenceRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return absenceService.enregistrer(request, principal.getUsername());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE') or @sec.isCenseur(authentication.name)")
    @Operation(summary = "Lister les absences d'une tontine")
    public List<AbsenceResponse> lister(@RequestParam UUID tontineId) {
        return absenceService.listerParTontine(tontineId);
    }

    @GetMapping("/mes-absences")
    @Operation(summary = "Mes absences")
    public List<AbsenceResponse> mesAbsences(@AuthenticationPrincipal UserDetails principal) {
        return absenceService.mesSAbsences(principal.getUsername());
    }
}
