package com.tontinepro.tontinepro_backend.api.setup;

import com.tontinepro.tontinepro_backend.api.auth.dto.AuthResponse;
import com.tontinepro.tontinepro_backend.api.setup.dto.SetupRequest;
import com.tontinepro.tontinepro_backend.api.setup.dto.SetupStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/setup")
@RequiredArgsConstructor
@Tag(name = "Setup")
public class SetupController {

    private final SetupService setupService;

    @GetMapping("/status")
    @Operation(summary = "Vérifie si la plateforme est déjà configurée")
    public SetupStatusResponse status() {
        return setupService.status();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Initialisation : crée la tontine + le compte Président fondateur")
    public AuthResponse setup(@Valid @RequestBody SetupRequest request) {
        return setupService.setup(request);
    }
}
