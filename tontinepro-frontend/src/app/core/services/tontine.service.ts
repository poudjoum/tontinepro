import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { TontineResponse, UpdateTontineConfigRequest } from '../models/tontine.model';

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
}
