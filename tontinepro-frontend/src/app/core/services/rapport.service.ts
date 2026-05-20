import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class RapportService {
  private api = `${environment.apiUrl}/rapports`;

  constructor(private http: HttpClient) {}

  mensuel(tontineId: string, mois: number, annee: number) {
    const params = new HttpParams()
      .set('tontineId', tontineId)
      .set('mois',      mois)
      .set('annee',     annee);
    return this.http.get(`${this.api}/mensuel`, { responseType: 'blob', params });
  }

  cotisations(tontineId: string, mois?: number | null, annee?: number) {
    let params = new HttpParams().set('tontineId', tontineId);
    if (mois)  params = params.set('mois',  mois);
    if (annee) params = params.set('annee', annee);
    return this.http.get(`${this.api}/cotisations`, { responseType: 'blob', params });
  }

  financier(tontineId: string) {
    const params = new HttpParams().set('tontineId', tontineId);
    return this.http.get(`${this.api}/financier`, { responseType: 'blob', params });
  }

  sanctions(tontineId: string, payee?: boolean) {
    let params = new HttpParams().set('tontineId', tontineId);
    if (payee !== undefined) params = params.set('payee', String(payee));
    return this.http.get(`${this.api}/sanctions`, { responseType: 'blob', params });
  }

  membres(tontineId: string) {
    const params = new HttpParams().set('tontineId', tontineId);
    return this.http.get(`${this.api}/membres`, { responseType: 'blob', params });
  }
}
