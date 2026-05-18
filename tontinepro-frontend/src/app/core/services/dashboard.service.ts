import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MembreDashboard, AdminDashboard } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private api = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getMembreDashboard() {
    return this.http.get<MembreDashboard>(`${this.api}/membre`);
  }

  getAdminDashboard() {
    return this.http.get<AdminDashboard>(`${this.api}/admin`);
  }
}
