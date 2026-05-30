import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { TontineResponse, UpdateTontineConfigRequest } from '../models/tontine.model';
import { AuthResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class TontineService {
  private api = `${environment.apiUrl}/tontines`;

  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<TontineResponse[]>(this.api);
  }

  getPubliques() {
    return this.http.get<TontineResponse[]>(`${this.api}/publiques`);
  }

  getById(id: string) {
    return this.http.get<TontineResponse>(`${this.api}/${id}`);
  }

  updateConfig(id: string, request: UpdateTontineConfigRequest) {
    return this.http.patch<TontineResponse>(`${this.api}/${id}/config`, request);
  }

  /** Rejoindre directement une tontine ouverte (utilisateur connecté) */
  rejoindreOuverte(tontineId: string, request: {
    nom: string; prenom: string; telephone?: string;
    typeParticipation?: 'TONTINE' | 'AIDE_SOCIALE';
    modePaiementAide?: 'MENSUEL' | 'EN_UNE_FOIS';
  }) {
    return this.http.post<any>(`${this.api}/${tontineId}/rejoindre`, request);
  }

  /** Création libre-service (public) : crée compte + tontine en une étape */
  creerAvecCompte(request: {
    email: string; password: string; nom: string; prenom: string; telephone?: string;
    tontineNom: string; tontineDescription?: string;
    montantCotisationMin: number; typeAcces?: string; descriptionAcces?: string;
  }) {
    return this.http.post<AuthResponse>(`${this.api}/creer-avec-compte`, request);
  }

  /** Création libre-service (authentifié) : pour un utilisateur connecté sans profil */
  creerMaTontine(request: {
    nom: string; description?: string; montantCotisationMin: number;
    typeAcces?: string; descriptionAcces?: string;
    membreNom: string; membrePrenom: string;
  }) {
    return this.http.post<TontineResponse>(`${this.api}/creer-ma-tontine`, request);
  }
}
