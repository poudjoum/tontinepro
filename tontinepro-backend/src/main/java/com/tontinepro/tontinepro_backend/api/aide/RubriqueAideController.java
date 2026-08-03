package com.tontinepro.tontinepro_backend.api.aide;

import com.tontinepro.tontinepro_backend.api.aide.dto.RubriqueAideRequest;
import com.tontinepro.tontinepro_backend.api.aide.dto.RubriqueAideResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rubriques-aide")
@RequiredArgsConstructor
@Tag(name = "Barème des aides")
public class RubriqueAideController {

    private final RubriqueAideService rubriqueAideService;

    @GetMapping("/{tontineId}")
    @Operation(summary = "Barème d'aide d'une tontine (actifSeulement=true pour la sélection membre)")
    public List<RubriqueAideResponse> lister(
            @PathVariable UUID tontineId,
            @RequestParam(required = false, defaultValue = "false") boolean actifSeulement
    ) {
        return rubriqueAideService.lister(tontineId, actifSeulement);
    }

    @PostMapping("/{tontineId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Créer une rubrique du barème d'aide")
    public RubriqueAideResponse creer(
            @PathVariable UUID tontineId,
            @Valid @RequestBody RubriqueAideRequest request
    ) {
        return rubriqueAideService.creer(tontineId, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Modifier une rubrique du barème d'aide")
    public RubriqueAideResponse modifier(
            @PathVariable UUID id,
            @Valid @RequestBody RubriqueAideRequest request
    ) {
        return rubriqueAideService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Supprimer une rubrique du barème d'aide")
    public void supprimer(@PathVariable UUID id) {
        rubriqueAideService.supprimer(id);
    }
}
