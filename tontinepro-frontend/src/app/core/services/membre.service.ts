import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { MembreResponse } from '../models/membre.model';

@Injectable({ providedIn: 'root' })
export class MembreService {
  private api = `${environment.apiUrl}/membres`;

  constructor(private http: HttpClient) {}

  getMonProfil() {
    return this.http.get<MembreResponse>(`${this.api}/me`);
  }
}
