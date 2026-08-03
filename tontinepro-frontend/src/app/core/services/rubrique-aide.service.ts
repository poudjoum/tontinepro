import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { RubriqueAideRequest, RubriqueAideResponse, SimulationAideResponse } from '../models/aide.model';

@Injectable({ providedIn: 'root' })
export class RubriqueAideService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/rubriques-aide`;

  lister(tontineId: string, actifSeulement = false) {
    const params = new HttpParams().set('actifSeulement', String(actifSeulement));
    return this.http.get<RubriqueAideResponse[]>(`${this.api}/${tontineId}`, { params });
  }

  creer(tontineId: string, request: RubriqueAideRequest) {
    return this.http.post<RubriqueAideResponse>(`${this.api}/${tontineId}`, request);
  }

  modifier(id: string, request: RubriqueAideRequest) {
    return this.http.put<RubriqueAideResponse>(`${this.api}/${id}`, request);
  }

  supprimer(id: string) {
    return this.http.delete<void>(`${this.api}/${id}`);
  }

  simuler(rubriqueId: string) {
    return this.http.get<SimulationAideResponse>(`${this.api}/simulation/${rubriqueId}`);
  }
}
