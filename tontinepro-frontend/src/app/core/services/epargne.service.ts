import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { CompteEpargneResponse, MouvementEpargneResponse } from '../models/epargne.model';

@Injectable({ providedIn: 'root' })
export class EpargneService {
  private api = `${environment.apiUrl}/epargne`;

  constructor(private http: HttpClient) {}

  getMonCompte(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<CompteEpargneResponse>(`${this.api}/mon-compte`, { params });
  }

  depot(montant: number, reference?: string) {
    return this.http.post<CompteEpargneResponse>(`${this.api}/depot`, { montant, reference });
  }

  retrait(montant: number, reference?: string) {
    return this.http.post<CompteEpargneResponse>(`${this.api}/retrait`, { montant, reference });
  }

  getHistorique(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<MouvementEpargneResponse[]>(`${this.api}/historique`, { params });
  }

  getAllComptes(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<CompteEpargneResponse[]>(`${this.api}/comptes`, { params });
  }

  getHistoriqueParMembre(membreId: string) {
    return this.http.get<MouvementEpargneResponse[]>(`${this.api}/comptes/${membreId}/historique`);
  }

  distribuerInterets(tontineId: string) {
    return this.http.post<{ comptesCredites: number }>(
      `${this.api}/distribuer-interets`, null,
      { params: new HttpParams().set('tontineId', tontineId) }
    );
  }
}
