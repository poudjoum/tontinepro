import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import {
  SessionResponse,
  SessionBilanResponse,
  CreerSessionRequest,
  MiseAJourDateRequest,
  ValiderBeneficeRequest,
  ReordonnerBeneficiairesRequest,
} from '../models/session.model';

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
}
