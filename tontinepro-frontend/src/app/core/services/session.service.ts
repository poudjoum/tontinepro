import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  SessionResponse,
  SessionBilanResponse,
  SessionCotisationsStatutResponse,
  OrdreBeneficiaireResponse,
  MonTourResponse,
  MonBeneficeResponse,
  RapportTourResponse,
  RapportFinSessionResponse,
  FondsAideMensuelResponse,
  MembreEligibleRetardResponse,
  InscrireEnRetardResult,
  CreerSessionRequest,
  MiseAJourDateRequest,
  ValiderBeneficeRequest,
  ReordonnerBeneficiairesRequest,
} from '../models/session.model';
import { CotisationResponse } from '../models/cotisation.model';
import { SessionLotResponse, CompositionLotsResponse, SaisieDepart } from '../models/tontine.model';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private api = `${environment.apiUrl}/sessions`;

  constructor(private http: HttpClient) {}

  creerSession(request: CreerSessionRequest) {
    return this.http.post<SessionResponse>(this.api, request);
  }

  listerSessions(tontineId: string) {
    const params = new HttpParams().set('tontineId', tontineId);
    return this.http.get<SessionResponse[]>(this.api, { params });
  }

  getById(id: string) {
    return this.http.get<SessionResponse>(`${this.api}/${id}`);
  }

  calculerBilan(sessionId: string) {
    return this.http.get<SessionBilanResponse>(`${this.api}/${sessionId}/bilan`);
  }

  mettreAJourProchainDate(id: string, request: MiseAJourDateRequest) {
    return this.http.patch<SessionResponse>(`${this.api}/${id}/prochaine-date`, request);
  }

  reordonnerBeneficiaires(id: string, request: ReordonnerBeneficiairesRequest) {
    return this.http.patch<SessionResponse>(`${this.api}/${id}/beneficiaires/reordonner`, request);
  }

  validerBenefice(sessionId: string, ordreBeneficiaireId: string, request: ValiderBeneficeRequest) {
    return this.http.post<SessionResponse>(
      `${this.api}/${sessionId}/valider-benefice/${ordreBeneficiaireId}`,
      request,
    );
  }

  recalibrerMembres(sessionId: string) {
    return this.http.post<SessionResponse>(`${this.api}/${sessionId}/recalibrer`, {});
  }

  annulerBenefice(sessionId: string, ordreBeneficiaireId: string) {
    return this.http.post<SessionResponse>(
      `${this.api}/${sessionId}/annuler-benefice/${ordreBeneficiaireId}`, {});
  }

  supprimerSession(sessionId: string) {
    return this.http.delete<void>(`${this.api}/${sessionId}`);
  }

  // ── Tontine à lot ──────────────────────────────────────────────────────────
  getLot(sessionId: string) {
    return this.http.get<SessionLotResponse>(`${this.api}/${sessionId}/lot`);
  }

  adhererLot(sessionId: string, membreId: string, montantMensuel: number) {
    return this.http.post<SessionLotResponse>(
      `${this.api}/${sessionId}/lot/adherer`, { membreId, montantMensuel });
  }

  /** Verse sa part à un co-bénéficiaire (retenue du fond de caisse incluse). */
  verserPartLot(sessionId: string, partId: string) {
    return this.http.post<SessionLotResponse>(`${this.api}/${sessionId}/lot/parts/${partId}/verser`, {});
  }

  annulerVersementPartLot(sessionId: string, partId: string) {
    return this.http.post<SessionLotResponse>(`${this.api}/${sessionId}/lot/parts/${partId}/annuler-versement`, {});
  }

  /** Départ d'un membre en cours de session à lot. */
  declarerAbandonLot(sessionId: string, membreId: string, dateAbandon: string | null, motif: string | null,
                     saisie: SaisieDepart) {
    return this.http.post<SessionLotResponse>(`${this.api}/${sessionId}/lot/abandons`,
      { membreId, dateAbandon, motif, ...saisie });
  }

  restituerAvoirLot(sessionId: string, abandonId: string) {
    return this.http.post<SessionLotResponse>(`${this.api}/${sessionId}/lot/abandons/${abandonId}/restituer`, {});
  }

  rembourserDetteLot(sessionId: string, abandonId: string, montant: number) {
    return this.http.post<SessionLotResponse>(
      `${this.api}/${sessionId}/lot/abandons/${abandonId}/remboursements`, { montant });
  }

  annulerAbandonLot(sessionId: string, abandonId: string) {
    return this.http.delete<SessionLotResponse>(`${this.api}/${sessionId}/lot/abandons/${abandonId}`);
  }

  /** Programme (ou modifie) le tour de remboursement des avoirs gelés. */
  programmerRemboursementLot(sessionId: string, dateTour: string,
                             cotisants: Array<{ membreId: string; montant: number }>) {
    return this.http.put<SessionLotResponse>(`${this.api}/${sessionId}/lot/remboursement`, { dateTour, cotisants });
  }

  encaisserRemboursementLot(sessionId: string, cotisantId: string, paye: boolean) {
    const action = paye ? 'encaisser' : 'annuler-encaissement';
    return this.http.post<SessionLotResponse>(
      `${this.api}/${sessionId}/lot/remboursement/cotisants/${cotisantId}/${action}`, {});
  }

  cloreRemboursementLot(sessionId: string) {
    return this.http.post<SessionLotResponse>(`${this.api}/${sessionId}/lot/remboursement/clore`, {});
  }

  rouvrirRemboursementLot(sessionId: string) {
    return this.http.post<SessionLotResponse>(`${this.api}/${sessionId}/lot/remboursement/rouvrir`, {});
  }

  supprimerRemboursementLot(sessionId: string) {
    return this.http.delete<SessionLotResponse>(`${this.api}/${sessionId}/lot/remboursement`);
  }

  /** Composition proposée (combinaisons exactes), non enregistrée. */
  getCompositionLot(sessionId: string) {
    return this.http.get<CompositionLotsResponse>(`${this.api}/${sessionId}/lot/composition`);
  }

  /** Fige avec la composition du bureau (automatique si `lots` absent). */
  figerLot(sessionId: string, lots?: Array<{ parts: Array<{ membreId: string; montant: number }> }>) {
    return this.http.post<SessionLotResponse>(`${this.api}/${sessionId}/lot/figer`, lots ? { lots } : {});
  }

  echeancier(sessionId: string) {
    return this.http.get<OrdreBeneficiaireResponse[]>(`${this.api}/${sessionId}/echeancier`);
  }

  monTour(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<MonTourResponse>(`${this.api}/mon-tour`, { params });
  }

  /** Corrige le montant réellement remis pour un tour déjà payé (le rapport n'est pas renvoyé). */
  corrigerMontantRecu(sessionId: string, ordreBeneficiaireId: string, montantRecu: number) {
    return this.http.patch<SessionResponse>(
      `${this.api}/${sessionId}/beneficiaires/${ordreBeneficiaireId}/montant-recu`, { montantRecu });
  }

  /** Caisse globale de la tontine à l'ouverture de la session. */
  enregistrerSoldeReporte(sessionId: string, montant: number) {
    return this.http.patch<SessionResponse>(`${this.api}/${sessionId}/solde-reporte`, { montant });
  }

  /** Avance sur un tour pas encore payé ; montant 0 = supprimer l'avance. */
  enregistrerAvance(sessionId: string, ordreBeneficiaireId: string, montant: number, dateAvance: string | null) {
    return this.http.patch<SessionResponse>(
      `${this.api}/${sessionId}/beneficiaires/${ordreBeneficiaireId}/avance`, { montant, dateAvance });
  }

  mettreAJourDateBenefice(sessionId: string, ordreBeneficiaireId: string, dateBenefice: string) {
    return this.http.patch<SessionResponse>(
      `${this.api}/${sessionId}/beneficiaires/${ordreBeneficiaireId}/date`,
      { dateBenefice }
    );
  }

  saisirPaiementsSeance(sessionId: string, paiements: Array<{
    cotisationId: string;
    referencePaiement?: string;
    montantTontine?: number;
    montantFondAide?: number;
    montantRepas?: number;
  }>, autoriserCorrection = false) {
    return this.http.post<{
      totalDemandes: number;
      totalEnregistres: number;
      totalDejaPayes: number;
      montantTontineCollecte: number;
      montantFondAideCollecte: number;
    }>(`${this.api}/${sessionId}/saisir-paiements-seance`, { paiements, autoriserCorrection });
  }

  cloturerSession(sessionId: string, forcer = false) {
    return this.http.post<SessionResponse>(`${this.api}/${sessionId}/cloturer`, null, {
      params: { forcer: String(forcer) }
    });
  }

  mesBenefices(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<MonBeneficeResponse[]>(`${this.api}/mes-benefices`, { params });
  }

  genererCotisations(sessionId: string, mois?: number, annee?: number) {
    let params = new HttpParams();
    if (mois  != null) params = params.set('mois', String(mois));
    if (annee != null) params = params.set('annee', String(annee));
    return this.http.post<CotisationResponse[]>(`${this.api}/${sessionId}/generer-cotisations`, {}, { params });
  }

  cotisationsStatut(sessionId: string, mois?: number, annee?: number) {
    let params = new HttpParams();
    if (mois  != null) params = params.set('mois', String(mois));
    if (annee != null) params = params.set('annee', String(annee));
    return this.http.get<SessionCotisationsStatutResponse>(`${this.api}/${sessionId}/cotisations-statut`, { params });
  }

  getRapportTour(sessionId: string, ordreBeneficiaireId: string) {
    return this.http.get<RapportTourResponse>(`${this.api}/${sessionId}/rapport-tour/${ordreBeneficiaireId}`);
  }

  getRapportFinSession(sessionId: string) {
    return this.http.get<RapportFinSessionResponse>(`${this.api}/${sessionId}/rapport-fin-session`);
  }

  getFondsAideMensuel(sessionId: string) {
    return this.http.get<FondsAideMensuelResponse>(`${this.api}/${sessionId}/fonds-aide-mensuel`);
  }

  telechargerFondsAideMensuelPdf(sessionId: string) {
    return this.http.get(`${this.api}/${sessionId}/fonds-aide-mensuel/pdf`, { responseType: 'blob' });
  }

  telechargerRapportFinSessionPdf(sessionId: string) {
    return this.http.get(`${this.api}/${sessionId}/rapport-fin-session/pdf`, { responseType: 'blob' });
  }

  membresEligiblesRetard(sessionId: string) {
    return this.http.get<MembreEligibleRetardResponse[]>(`${this.api}/${sessionId}/membres-eligibles-retard`);
  }

  inscrireEnRetard(sessionId: string, membreId: string,
                   montantCotisationParTour?: number,
                   montantRepasParTour?: number,
                   montantFondAideParTour?: number) {
    const body: Record<string, number> = {};
    if (montantCotisationParTour != null) body['montantCotisationParTour'] = montantCotisationParTour;
    if (montantRepasParTour      != null) body['montantRepasParTour']      = montantRepasParTour;
    if (montantFondAideParTour   != null) body['montantFondAideParTour']   = montantFondAideParTour;
    return this.http.post<InscrireEnRetardResult>(
      `${this.api}/${sessionId}/inscrire-en-retard/${membreId}`, body);
  }
}
