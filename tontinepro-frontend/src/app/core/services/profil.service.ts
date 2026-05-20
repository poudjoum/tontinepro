import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MembreResponse } from '../models/membre.model';

export interface UpdateProfilRequest {
  nom?: string;
  prenom?: string;
  telephone?: string;
}

export interface ChangerMotDePasseRequest {
  ancienMotDePasse: string;
  nouveauMotDePasse: string;
}

@Injectable({ providedIn: 'root' })
export class ProfilService {
  private api = `${environment.apiUrl}/profil`;

  constructor(private http: HttpClient) {}

  mettreAJour(request: UpdateProfilRequest) {
    return this.http.patch<MembreResponse>(`${this.api}/me`, request);
  }

  changerMotDePasse(request: ChangerMotDePasseRequest) {
    return this.http.post<void>(`${this.api}/me/changer-mot-de-passe`, request);
  }
}
