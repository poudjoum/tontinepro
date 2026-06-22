import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MembreDashboard, AdminDashboard } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private api = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getMembreDashboard(tontineId?: string) {
    return this.http.get<MembreDashboard>(`${this.api}/membre`, { params: this.params(tontineId) });
  }

  getAdminDashboard(tontineId?: string) {
    return this.http.get<AdminDashboard>(`${this.api}/admin`, { params: this.params(tontineId) });
  }

  private params(tontineId?: string): HttpParams {
    let p = new HttpParams();
    if (tontineId) p = p.set('tontineId', tontineId);
    return p;
  }
}
