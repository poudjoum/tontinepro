import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { DemandeResponse, DocumentTontineResponse, SoumettreDemandeRequest, StatutDemande, TypeDocumentTontine } from '../models/demande.model';

@Injectable({ providedIn: 'root' })
export class DemandeService {
  private api = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  // Public
  soumettre(tontineId: string, request: SoumettreDemandeRequest) {
    return this.http.post<DemandeResponse>(`${this.api}/tontines/${tontineId}/demandes`, request);
  }

  documentsOfficielsTontine(tontineId: string) {
    return this.http.get<DocumentTontineResponse[]>(`${this.api}/tontines/${tontineId}/documents-officiels`);
  }

  urlDocumentOfficiel(tontineId: string, docId: string): string {
    return `${this.api}/tontines/${tontineId}/documents-officiels/${docId}/fichier`;
  }

  // Admin
  lister(tontineId?: string, statut?: StatutDemande) {
    const params: Record<string, string> = {};
    if (tontineId) params['tontineId'] = tontineId;
    if (statut) params['statut'] = statut;
    return this.http.get<DemandeResponse[]>(`${this.api}/demandes`, { params });
  }

  approuver(id: string) {
    return this.http.post<DemandeResponse>(`${this.api}/demandes/${id}/approuver`, {});
  }

  rejeter(id: string, motifRejet: string) {
    return this.http.post<DemandeResponse>(`${this.api}/demandes/${id}/rejeter`, { motifRejet });
  }

  uploadDocumentOfficiel(tontineId: string, typeDocument: TypeDocumentTontine, fichier: File) {
    const fd = new FormData();
    fd.append('typeDocument', typeDocument);
    fd.append('fichier', fichier);
    return this.http.post<DocumentTontineResponse>(
      `${this.api}/tontines/${tontineId}/documents-officiels`, fd);
  }

  supprimerDocumentOfficiel(tontineId: string, docId: string) {
    return this.http.delete<void>(`${this.api}/tontines/${tontineId}/documents-officiels/${docId}`);
  }
}
