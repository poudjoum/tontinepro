import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { PretResponse, EcheancePretResponse, SimulationPretResponse } from '../models/pret.model';

@Injectable({ providedIn: 'root' })
export class PretService {
  private api = `${environment.apiUrl}/prets`;

  constructor(private http: HttpClient) {}

  getMesPrets(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<PretResponse[]>(`${this.api}/mes-prets`, { params });
  }

  getAll(statut?: string) {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return this.http.get<PretResponse[]>(this.api, { params });
  }

  getEcheances(pretId: string) {
    return this.http.get<EcheancePretResponse[]>(`${this.api}/${pretId}/echeances`);
  }

  demande(montantPrincipal: number, dureeMois: number) {
    return this.http.post<PretResponse>(`${this.api}/demande`, { montantPrincipal, dureeMois });
  }

  simuler(montant: number, duree: number, tontineId: string) {
    const params = new HttpParams()
      .set('montant', montant)
      .set('duree',   duree)
      .set('tontineId', tontineId);
    return this.http.get<SimulationPretResponse>(`${this.api}/simulation`, { params });
  }

  rembourser(pretId: string) {
    return this.http.post<EcheancePretResponse>(`${this.api}/${pretId}/rembourser`, {});
  }

  valider(pretId: string) {
    return this.http.patch<PretResponse>(`${this.api}/${pretId}/valider`, {});
  }

  rejeter(pretId: string, motif: string) {
    return this.http.patch<PretResponse>(`${this.api}/${pretId}/rejeter`, { motif });
  }
}
