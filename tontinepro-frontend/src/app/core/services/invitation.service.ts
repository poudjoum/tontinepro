import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { GenererInvitationRequest, InvitationResponse, RejoindreRequest, StatutInvitationResponse } from '../models/invitation.model';
import { AuthResponse } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class InvitationService {
  private api = `${environment.apiUrl}/invitation`;

  constructor(private http: HttpClient) {}

  generer(request: GenererInvitationRequest) {
    return this.http.post<InvitationResponse>(`${this.api}/generer`, request);
  }

  statut(token: string) {
    return this.http.get<StatutInvitationResponse>(`${this.api}/${token}/statut`);
  }

  rejoindre(token: string, request: RejoindreRequest) {
    return this.http.post<AuthResponse>(`${this.api}/${token}/rejoindre`, request);
  }

  lister(tontineId?: string) {
    if (tontineId) {
      return this.http.get<InvitationResponse[]>(this.api, { params: { tontineId } });
    }
    return this.http.get<InvitationResponse[]>(this.api);
  }
}
