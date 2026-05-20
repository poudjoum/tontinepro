import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

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
  emailSecretaire: string | null;
  redevance: RedevanceInfo | null;
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
