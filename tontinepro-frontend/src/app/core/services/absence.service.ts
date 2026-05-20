import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { AbsenceResponse, AppelPresenceRequest, AppelPresenceResult, EnregistrerAbsenceRequest } from '../models/absence.model';

@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private api = `${environment.apiUrl}/absences`;

  constructor(private http: HttpClient) {}

  enregistrer(request: EnregistrerAbsenceRequest) {
    return this.http.post<AbsenceResponse>(this.api, request);
  }

  appelPresence(request: AppelPresenceRequest) {
    return this.http.post<AppelPresenceResult>(`${this.api}/appel-presence`, request);
  }

  lister(tontineId: string) {
    return this.http.get<AbsenceResponse[]>(this.api, { params: { tontineId } });
  }

  mesAbsences() {
    return this.http.get<AbsenceResponse[]>(`${this.api}/mes-absences`);
  }
}
