package com.tontinepro.tontinepro_backend.api.notification;

import com.tontinepro.tontinepro_backend.api.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Mes notifications (les plus récentes en premier)")
    public List<NotificationResponse> getMesNotifications(@AuthenticationPrincipal UserDetails principal) {
        return notificationService.getMesNotifications(principal.getUsername());
    }

    @GetMapping("/non-lues/count")
    @Operation(summary = "Nombre de notifications non lues")
    public Map<String, Long> countNonLues(@AuthenticationPrincipal UserDetails principal) {
        return Map.of("nonLues", notificationService.countNonLues(principal.getUsername()));
    }

    @PatchMapping("/{id}/lire")
    @Operation(summary = "Marquer une notification comme lue")
    public NotificationResponse marquerLue(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return notificationService.marquerLue(id, principal.getUsername());
    }

    @PatchMapping("/lire-tout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marquer toutes les notifications comme lues")
    public void marquerToutesLues(@AuthenticationPrincipal UserDetails principal) {
        notificationService.marquerToutesLues(principal.getUsername());
    }
}
