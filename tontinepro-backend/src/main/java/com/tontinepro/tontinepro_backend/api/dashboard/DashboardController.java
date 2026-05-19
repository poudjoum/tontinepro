package com.tontinepro.tontinepro_backend.api.dashboard;

import com.tontinepro.tontinepro_backend.api.dashboard.dto.AdminDashboardResponse;
import com.tontinepro.tontinepro_backend.api.dashboard.dto.MembreDashboardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/membre")
    @Operation(summary = "Tableau de bord personnel du membre")
    public MembreDashboardResponse getMembreDashboard(
            @AuthenticationPrincipal UserDetails principal) {
        return dashboardService.getMembreDashboard(principal.getUsername());
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Tableau de bord administrateur")
    public AdminDashboardResponse getAdminDashboard(
            @AuthenticationPrincipal UserDetails principal) {
        return dashboardService.getAdminDashboard(principal.getUsername());
    }
}

