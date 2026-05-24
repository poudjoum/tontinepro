import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

export interface RedevanceInfo {
  id: string;
  montant: number;
  periodicite: 'MENSUEL' | 'ANNUEL';
  statut: 'A_JOUR' | 'EN_RETARD' | 'GRATUIT';
  prochainPaiement: string | null;
}

export interface TontinePlatformeResponse {
  id: string;
  nom: string;
  description: string | null;
  actif: boolean;
  nombreMembresActifs: number;
  emailPresident: string | null;
  telephonePresident: string | null;
  nomPresident: string | null;
  prenomPresident: string | null;
  emailSecretaire: string | null;
  telephoneSecretaire: string | null;
  nomSecretaire: string | null;
  prenomSecretaire: string | null;
  redevance: RedevanceInfo | null;
}

export interface SuperAdminCreerTontineRequest {
  tontineNom: string;
  tontineDescription?: string;
  montantCotisationMin: number;
  typeAcces?: 'OUVERTE' | 'RESTREINTE';
  descriptionAcces?: string;
  secretaireEmail: string;
  secretaireNom: string;
  secretairePrenom: string;
  secretaireTelephone?: string;
  secretairePassword: string;
}

export interface ConfigurerRedevanceRequest {
  montant: number;
  periodicite: 'MENSUEL' | 'ANNUEL';
  statut: 'A_JOUR' | 'EN_RETARD' | 'GRATUIT';
  prochainPaiement?: string;
}

@Injectable({ providedIn: 'root' })
export class SuperAdminService {
  private api = `${environment.apiUrl}/super-admin`;

  constructor(private http: HttpClient) {}

  creerTontine(request: SuperAdminCreerTontineRequest): Observable<TontinePlatformeResponse> {
    return this.http.post<TontinePlatformeResponse>(`${this.api}/tontines`, request);
  }

  listerTontines() {
    return this.http.get<TontinePlatformeResponse[]>(`${this.api}/tontines`);
  }

  toggleActif(tontineId: string, actif: boolean) {
    return this.http.patch<TontinePlatformeResponse>(
      `${this.api}/tontines/${tontineId}/actif`, null, { params: { actif: String(actif) } }
    );
  }

  configurerRedevance(tontineId: string, request: ConfigurerRedevanceRequest) {
    return this.http.put<TontinePlatformeResponse>(
      `${this.api}/tontines/${tontineId}/redevance`, request
    );
  }

  reinitialiserMotDePasse(tontineId: string) {
    return this.http.post<{ message: string; nbEnvois: number }>(
      `${this.api}/tontines/${tontineId}/reset-password`, {}
    );
  }
}
