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
  MembreEligibleRetardResponse,
  InscrireEnRetardResult,
  CreerSessionRequest,
  MiseAJourDateRequest,
  ValiderBeneficeRequest,
  ReordonnerBeneficiairesRequest,
} from '../models/session.model';
import { CotisationResponse } from '../models/cotisation.model';
import { SessionLotResponse } from '../models/tontine.model';

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

  figerLot(sessionId: string) {
    return this.http.post<SessionLotResponse>(`${this.api}/${sessionId}/lot/figer`, {});
  }

  echeancier(sessionId: string) {
    return this.http.get<OrdreBeneficiaireResponse[]>(`${this.api}/${sessionId}/echeancier`);
  }

  monTour(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<MonTourResponse>(`${this.api}/mon-tour`, { params });
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

  mesBenefices() {
    return this.http.get<MonBeneficeResponse[]>(`${this.api}/mes-benefices`);
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
