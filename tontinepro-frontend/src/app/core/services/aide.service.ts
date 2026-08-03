import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AideResponse, AideSuiviResponse, TypeAide } from '../models/aide.model';

@Injectable({ providedIn: 'root' })
export class AideService {
  private api = `${environment.apiUrl}/aides`;

  constructor(private http: HttpClient) {}

  getMesDemandes(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<AideResponse[]>(`${this.api}/mes-demandes`, { params });
  }

  getAll(statut?: string) {
    let params = new HttpParams();
    if (statut) params = params.set('statut', statut);
    return this.http.get<AideResponse[]>(`${this.api}/demandes`, { params });
  }

  soumettre(typeAide: TypeAide, montantDemande: number, motif: string, justificatifUrl?: string) {
    return this.http.post<AideResponse>(`${this.api}/demandes`, {
      typeAide, montantDemande, motif, justificatifUrl,
    });
  }

  soumettreDepuisRubrique(rubriqueId: string, motif: string, justificatifUrl?: string) {
    return this.http.post<AideResponse>(`${this.api}/demandes`, {
      rubriqueId, motif, justificatifUrl,
    });
  }

  saisirPourMembre(membreId: string, rubriqueId: string, motif: string, justificatifUrl?: string) {
    return this.http.post<AideResponse>(`${this.api}/saisir`, {
      membreId, rubriqueId, motif, justificatifUrl,
    });
  }

  accepter(id: string) {
    return this.http.patch<AideResponse>(`${this.api}/demandes/${id}/accepter`, {});
  }

  refuser(id: string) {
    return this.http.patch<AideResponse>(`${this.api}/demandes/${id}/refuser`, {});
  }

  valider(id: string, montantAccorde: number) {
    return this.http.patch<AideResponse>(`${this.api}/demandes/${id}/valider`, { montantAccorde });
  }

  rejeter(id: string, motifRejet: string) {
    return this.http.patch<AideResponse>(`${this.api}/demandes/${id}/rejeter`, { motifRejet });
  }

  marquerPayee(id: string) {
    return this.http.patch<AideResponse>(`${this.api}/demandes/${id}/payer`, {});
  }

  activer(id: string, prefinance: boolean) {
    const params = new HttpParams().set('prefinance', String(prefinance));
    return this.http.post<AideResponse>(`${this.api}/demandes/${id}/activer`, {}, { params });
  }

  verser(id: string) {
    return this.http.post<AideResponse>(`${this.api}/demandes/${id}/verser`, {});
  }

  getSuivi(id: string) {
    return this.http.get<AideSuiviResponse>(`${this.api}/demandes/${id}/suivi`);
  }
}
