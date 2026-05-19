import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { CreerSanctionRequest, SanctionResponse } from '../models/sanction.model';

@Injectable({ providedIn: 'root' })
export class SanctionService {
  private api = `${environment.apiUrl}/sanctions`;

  constructor(private http: HttpClient) {}

  creer(request: CreerSanctionRequest) {
    return this.http.post<SanctionResponse>(this.api, request);
  }

  lister(tontineId: string, payee?: boolean) {
    const params: Record<string, string> = { tontineId };
    if (payee !== undefined) params['payee'] = String(payee);
    return this.http.get<SanctionResponse[]>(this.api, { params });
  }

  mesSanctions() {
    return this.http.get<SanctionResponse[]>(`${this.api}/mes-sanctions`);
  }

  marquerPayee(id: string) {
    return this.http.patch<SanctionResponse>(`${this.api}/${id}/payer`, {});
  }
}
