package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.cotisation.dto.CotisationResponse;
import com.tontinepro.tontinepro_backend.api.session.dto.*;
import com.tontinepro.tontinepro_backend.api.session.dto.MonBeneficeResponse;
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
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Sessions de tontine")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Creer une nouvelle session de tontine")
    public SessionResponse creerSession(@Valid @RequestBody CreerSessionRequest request) {
        return sessionService.creerSession(request);
    }

    @GetMapping
    @Operation(summary = "Lister les sessions d'une tontine")
    public List<SessionResponse> listerSessions(@RequestParam UUID tontineId) {
        return sessionService.listerSessions(tontineId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Details d'une session avec la liste ordonnee des beneficiaires")
    public SessionResponse getById(@PathVariable UUID id) {
        return sessionService.getById(id);
    }

    /** Calendrier d'echéances complet d'une session */
    @GetMapping("/{id}/echeancier")
    @Operation(summary = "Calendrier d'echeances de la session (tous les beneficiaires avec dates)")
    public List<OrdreBeneficiaireResponse> echeancier(@PathVariable UUID id) {
        return sessionService.echeancier(id);
    }

    /** Mon tour — vue membre */
    @GetMapping("/mon-tour")
    @Operation(summary = "Ma date de passage dans la session en cours (vue membre)")
    public MonTourResponse monTour(@AuthenticationPrincipal UserDetails principal) {
        return sessionService.monTour(principal.getUsername());
    }

    @PatchMapping("/{id}/prochaine-date")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Mettre a jour la prochaine date de tontine (mode DATE_MANUELLE)")
    public SessionResponse mettreAJourProchainDate(
            @PathVariable UUID id,
            @Valid @RequestBody MiseAJourDateRequest request) {
        return sessionService.mettreAJourProchainDate(id, request);
    }

    /** Modifier la date de benefice d'un membre specifique */
    @PatchMapping("/{id}/beneficiaires/{ordreBeneficiaireId}/date")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Modifier la date de passage d'un beneficiaire (avant echeance)")
    public SessionResponse mettreAJourDateBenefice(
            @PathVariable UUID id,
            @PathVariable UUID ordreBeneficiaireId,
            @Valid @RequestBody MettreAJourDateBeneficeRequest request) {
        return sessionService.mettreAJourDateBenefice(id, ordreBeneficiaireId, request);
    }

    @PatchMapping("/{id}/beneficiaires/reordonner")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Reordonner la liste des beneficiaires d'une session")
    public SessionResponse reordonnerBeneficiaires(
            @PathVariable UUID id,
            @Valid @RequestBody ReordonnerBeneficiairesRequest request) {
        return sessionService.reordonnerBeneficiaires(id, request);
    }

    @GetMapping("/{id}/bilan")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Bilan financier : pot tontine, fonds collecte, deduction beneficiaire")
    public SessionBilanResponse calculerBilan(@PathVariable UUID id) {
        return sessionService.calculerBilan(id);
    }

    @PostMapping("/{id}/recalibrer")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Ajouter les nouveaux membres a la session et recalculer dateFin")
    public SessionResponse recalibrerMembres(@PathVariable UUID id) {
        return sessionService.recalibrerMembres(id);
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Clôturer une session (passe à TERMINEE). forcer=true permet de clôturer même si des membres n'ont pas bénéficié")
    public SessionResponse cloturerSession(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean forcer) {
        return sessionService.cloturerSession(id, forcer);
    }

    @GetMapping("/mes-benefices")
    @Operation(summary = "Historique des bénéfices reçus par le membre connecté")
    public List<MonBeneficeResponse> mesBenefices(@AuthenticationPrincipal UserDetails principal) {
        return sessionService.mesBenefices(principal.getUsername());
    }

    @PostMapping("/{id}/generer-cotisations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Générer les cotisations EN_ATTENTE pour tous les membres de la session (idempotent)")
    public List<CotisationResponse> genererCotisations(@PathVariable UUID id) {
        return sessionService.genererCotisations(id);
    }

    @GetMapping("/{id}/cotisations-statut")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Statut des cotisations par membre pour la période de la session")
    public SessionCotisationsStatutResponse cotisationsStatut(@PathVariable UUID id) {
        return sessionService.cotisationsStatut(id);
    }

    @PostMapping("/{id}/valider-benefice/{ordreBeneficiaireId}")
    @PreAuthorize("hasAnyRole('ADMIN','SECRETAIRE')")
    @Operation(summary = "Valider le benefice d'un membre dans la session")
    public SessionResponse validerBenefice(
            @PathVariable UUID id,
            @PathVariable UUID ordreBeneficiaireId,
            @Valid @RequestBody ValiderBeneficeRequest request) {
        return sessionService.validerBenefice(id, ordreBeneficiaireId, request);
    }
}
