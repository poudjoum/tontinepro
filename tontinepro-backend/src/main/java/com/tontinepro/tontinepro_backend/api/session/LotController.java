package com.tontinepro.tontinepro_backend.api.session;

import com.tontinepro.tontinepro_backend.api.session.dto.AdhererLotRequest;
import com.tontinepro.tontinepro_backend.api.session.dto.CompositionLotsResponse;
import com.tontinepro.tontinepro_backend.api.session.dto.DeclarerAbandonRequest;
import com.tontinepro.tontinepro_backend.api.session.dto.RemboursementDetteRequest;
import com.tontinepro.tontinepro_backend.api.session.dto.FigerLotRequest;
import com.tontinepro.tontinepro_backend.api.session.dto.SessionLotResponse;
import com.tontinepro.tontinepro_backend.api.session.dto.TourRemboursementRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/lot")
@RequiredArgsConstructor
@Tag(name = "Tontine à lot")
public class LotController {

    private final LotService lotService;
    private final SessionService sessionService;
    private final AbandonLotService abandonLotService;
    private final TourRestitutionService tourRestitutionService;

    @GetMapping
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Vue d'une session à lot : adhésions et lots/tours")
    public SessionLotResponse getLot(@PathVariable UUID sessionId) {
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/adherer")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Inscrire/mettre à jour la mise mensuelle d'un membre (avant figeage)")
    public SessionLotResponse adherer(@PathVariable UUID sessionId,
                                      @Valid @RequestBody AdhererLotRequest request) {
        lotService.adherer(sessionId, request.membreId(), request.montantMensuel());
        return lotService.getLotView(sessionId);
    }

    @GetMapping("/composition")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Proposer une composition des lots (combinaisons exactes), sans l'enregistrer")
    public CompositionLotsResponse composition(@PathVariable UUID sessionId) {
        return lotService.apercu(sessionId);
    }

    @PostMapping("/parts/{partId}/verser")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Verser sa part à un co-bénéficiaire (retenue du fond de caisse incluse)")
    public SessionLotResponse verserPart(@PathVariable UUID sessionId, @PathVariable UUID partId) {
        var tour = lotService.verserPart(sessionId, partId);
        if (tour.isBeneficie()) {
            sessionService.envoyerRapportTour(sessionId, tour.getId());   // dernière part : tour clos
        }
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/parts/{partId}/annuler-versement")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Annuler le versement d'une part (correction)")
    public SessionLotResponse annulerVersementPart(@PathVariable UUID sessionId, @PathVariable UUID partId) {
        lotService.annulerVersementPart(sessionId, partId);
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/abandons")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Déclarer l'abandon d'un membre : avoir gelé (cotisé − reçu) ou dette")
    public SessionLotResponse declarerAbandon(@PathVariable UUID sessionId,
                                              @Valid @RequestBody DeclarerAbandonRequest request) {
        abandonLotService.declarer(sessionId, request.membreId(), request.dateAbandon(), request.motif(),
                new AbandonLotService.Saisie(request.moisCotises(), request.totalCotise(), request.totalRecu()));
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/abandons/{abandonId}/restituer")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Restituer l'avoir gelé d'un membre parti (fin de session)")
    public SessionLotResponse restituerAvoir(@PathVariable UUID sessionId, @PathVariable UUID abandonId) {
        abandonLotService.restituer(sessionId, abandonId);
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/abandons/{abandonId}/remboursements")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Enregistrer un remboursement sur la dette d'un membre parti après son tour")
    public SessionLotResponse rembourserDette(@PathVariable UUID sessionId, @PathVariable UUID abandonId,
                                              @Valid @RequestBody RemboursementDetteRequest request) {
        abandonLotService.rembourser(sessionId, abandonId, request.montant());
        return lotService.getLotView(sessionId);
    }

    @DeleteMapping("/abandons/{abandonId}")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Annuler un abandon saisi par erreur")
    public SessionLotResponse annulerAbandon(@PathVariable UUID sessionId, @PathVariable UUID abandonId) {
        abandonLotService.annuler(sessionId, abandonId);
        return lotService.getLotView(sessionId);
    }

    @PutMapping("/remboursement")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Programmer le tour de remboursement des avoirs gelés : date et cotisants")
    public SessionLotResponse programmerRemboursement(@PathVariable UUID sessionId,
                                                      @Valid @RequestBody TourRemboursementRequest request) {
        tourRestitutionService.programmer(sessionId, request.dateTour(), request.cotisants().stream()
                .map(c -> new TourRestitutionService.Cotisant(c.membreId(), c.montant())).toList());
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/remboursement/cotisants/{cotisantId}/encaisser")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Encaisser la cotisation d'un membre au tour de remboursement")
    public SessionLotResponse encaisserRemboursement(@PathVariable UUID sessionId, @PathVariable UUID cotisantId) {
        tourRestitutionService.encaisser(sessionId, cotisantId, true);
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/remboursement/cotisants/{cotisantId}/annuler-encaissement")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Annuler l'encaissement d'une cotisation au tour de remboursement")
    public SessionLotResponse annulerEncaissementRemboursement(@PathVariable UUID sessionId,
                                                               @PathVariable UUID cotisantId) {
        tourRestitutionService.encaisser(sessionId, cotisantId, false);
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/remboursement/clore")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Clore le tour de remboursement : rend tous les avoirs gelés")
    public SessionLotResponse cloreRemboursement(@PathVariable UUID sessionId) {
        tourRestitutionService.clore(sessionId);
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/remboursement/rouvrir")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Rouvrir le tour de remboursement : les avoirs rendus redeviennent gelés")
    public SessionLotResponse rouvrirRemboursement(@PathVariable UUID sessionId) {
        tourRestitutionService.rouvrir(sessionId);
        return lotService.getLotView(sessionId);
    }

    @DeleteMapping("/remboursement")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Supprimer le tour de remboursement (pas encore clos)")
    public SessionLotResponse supprimerRemboursement(@PathVariable UUID sessionId) {
        tourRestitutionService.supprimer(sessionId);
        return lotService.getLotView(sessionId);
    }

    @PostMapping("/figer")
    @PreAuthorize("@sec.gere(authentication.name, @tontineDe.session(#sessionId))")
    @Operation(summary = "Figer la session avec la composition du bureau (ou automatique si absente)")
    public SessionLotResponse figer(@PathVariable UUID sessionId,
                                    @Valid @RequestBody(required = false) FigerLotRequest request) {
        lotService.figer(sessionId, request);
        return lotService.getLotView(sessionId);
    }
}
