import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MembreResponse } from '../models/membre.model';
import { SuiviMois } from '../models/suivi.model';

@Injectable({ providedIn: 'root' })
export class MembreService {
  private api = `${environment.apiUrl}/membres`;

  constructor(private http: HttpClient) {}

  getMonProfil(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<MembreResponse>(`${this.api}/me`, { params });
  }

  getAll(tontineId?: string, statut?: string) {
    const params: Record<string, string> = {};
    if (tontineId) params['tontineId'] = tontineId;
    if (statut) params['statut'] = statut;
    return this.http.get<MembreResponse[]>(this.api, { params });
  }

  getById(id: string) {
    return this.http.get<MembreResponse>(`${this.api}/${id}`);
  }

  updateStatut(id: string, statut: string) {
    return this.http.patch<MembreResponse>(`${this.api}/${id}/statut`, { statut });
  }

  updateFonction(id: string, fonction: string) {
    return this.http.patch<MembreResponse>(`${this.api}/${id}/fonction`, { fonction });
  }

  getSuivi(annee?: number, tontineId?: string) {
    const params: Record<string, string> = {};
    if (annee) params['annee'] = String(annee);
    if (tontineId) params['tontineId'] = tontineId;
    return this.http.get<SuiviMois[]>(`${this.api}/me/suivi`, { params });
  }
}
