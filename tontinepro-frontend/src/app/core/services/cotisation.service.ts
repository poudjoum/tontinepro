import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { CotisationResponse, EnregistrerPaiementRequest, ModifierCotisationRequest } from '../models/cotisation.model';

@Injectable({ providedIn: 'root' })
export class CotisationService {
  private api = `${environment.apiUrl}/cotisations`;

  constructor(private http: HttpClient) {}

  getMesCotisations() {
    return this.http.get<CotisationResponse[]>(`${this.api}/me`);
  }

  getAll(mois?: number, annee?: number, statut?: string) {
    let params = new HttpParams();
    if (mois)   params = params.set('mois',   mois);
    if (annee)  params = params.set('annee',  annee);
    if (statut) params = params.set('statut', statut);
    return this.http.get<CotisationResponse[]>(this.api, { params });
  }

  enregistrerPaiement(id: string, req: EnregistrerPaiementRequest) {
    return this.http.patch<CotisationResponse>(`${this.api}/${id}/paiement`, req);
  }

  marquerEnRetard(id: string) {
    return this.http.patch<CotisationResponse>(`${this.api}/${id}/retard`, {});
  }

  getById(id: string) {
    return this.http.get<CotisationResponse>(`${this.api}/${id}`);
  }

  modifier(id: string, req: ModifierCotisationRequest) {
    return this.http.patch<CotisationResponse>(`${this.api}/${id}`, req);
  }
}
