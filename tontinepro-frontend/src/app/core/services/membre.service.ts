import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MembreResponse } from '../models/membre.model';

@Injectable({ providedIn: 'root' })
export class MembreService {
  private api = `${environment.apiUrl}/membres`;

  constructor(private http: HttpClient) {}

  getMonProfil() {
    return this.http.get<MembreResponse>(`${this.api}/me`);
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
}
