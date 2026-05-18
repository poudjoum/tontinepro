import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { shareReplay, map } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse } from '../models/auth.model';

export interface SetupStatus { configured: boolean; }

export interface SetupRequest {
  nom: string;
  prenom: string;
  email: string;
  password: string;
  telephone?: string;
  tontineNom: string;
  tontineDescription?: string;
  montantCotisation: number;
  jourCotisation: number;
  tauxInteretPret?: number;
  tauxInteretEpargne?: number;
}

@Injectable({ providedIn: 'root' })
export class SetupService {
  private api = `${environment.apiUrl}/setup`;
  private status$?: Observable<SetupStatus>;

  constructor(private http: HttpClient) {}

  getStatus(): Observable<SetupStatus> {
    if (!this.status$) {
      this.status$ = this.http.get<SetupStatus>(`${this.api}/status`).pipe(
        shareReplay(1)
      );
    }
    return this.status$;
  }

  isConfigured(): Observable<boolean> {
    return this.getStatus().pipe(map(s => s.configured));
  }

  setup(req: SetupRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(this.api, req);
  }
}
