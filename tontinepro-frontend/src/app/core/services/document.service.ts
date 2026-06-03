import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { DocumentResponse, TypeDocument } from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private api = `${environment.apiUrl}/documents`;

  constructor(private http: HttpClient) {}

  telecharger(membreId: string, typeDocument: TypeDocument, fichier: File) {
    const fd = new FormData();
    fd.append('membreId', membreId);
    fd.append('typeDocument', typeDocument);
    fd.append('fichier', fichier);
    return this.http.post<DocumentResponse>(this.api, fd);
  }

  listerParMembre(membreId: string) {
    return this.http.get<DocumentResponse[]>(`${this.api}/membre/${membreId}`);
  }

  mesDocuments(tontineId?: string) {
    let params = new HttpParams();
    if (tontineId) params = params.set('tontineId', tontineId);
    return this.http.get<DocumentResponse[]>(`${this.api}/mes-documents`, { params });
  }

  urlFichier(id: string): string {
    return `${this.api}/${id}/fichier`;
  }

  supprimer(id: string) {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
