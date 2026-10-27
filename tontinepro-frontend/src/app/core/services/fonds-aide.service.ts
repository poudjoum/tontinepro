import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ArriereMembre, VersementsAnterieursRequest, VersementsAnterieursResponse } from '../models/aide.model';

/**
 * Fonds d'aide (trésorerie de solidarité) : enregistrement du paiement des
 * contributions des membres. Le solde du fonds est recrédité à chaque paiement.
 */
@Injectable({ providedIn: 'root' })
export class FondsAideService {
  private http = inject(HttpClient);
  private api = `${environment.apiUrl}/fonds-aide`;

  payerContribution(contributionId: string) {
    return this.http.patch(`${this.api}/contributions/${contributionId}/payer`, {});
  }

  /** Encaisse le rattrapage de fin de session d'un membre (ses parts dues sur ces aides). */
  encaisserRattrapage(tontineId: string, membreId: string, aideIds: string[]) {
    return this.http.post<{ montantEncaisse: number }>(
      `${this.api}/${tontineId}/rattrapage`, { membreId, aideIds });
  }

  /** Fond de caisse versé avant l'application, par membre, pour une année. */
  getVersementsAnterieurs(tontineId: string, annee: number) {
    return this.http.get<VersementsAnterieursResponse>(
      `${this.api}/${tontineId}/versements-anterieurs`, { params: { annee } });
  }

  /** Arriérés des membres (fonds non cotisé avant l'application). */
  getArrieres(tontineId: string) {
    return this.http.get<ArriereMembre[]>(`${this.api}/${tontineId}/arrieres`);
  }

  creerArriere(tontineId: string, req: { membreId: string; libelle: string | null; montant: number; dateConstat: string | null }) {
    return this.http.post<ArriereMembre[]>(`${this.api}/${tontineId}/arrieres`, req);
  }

  supprimerArriere(tontineId: string, arriereId: string) {
    return this.http.delete<ArriereMembre[]>(`${this.api}/${tontineId}/arrieres/${arriereId}`);
  }

  reglerArriere(tontineId: string, arriereId: string, montant: number, dateReglement: string | null) {
    return this.http.post<ArriereMembre[]>(
      `${this.api}/${tontineId}/arrieres/${arriereId}/reglements`, { montant, dateReglement });
  }

  annulerReglementArriere(tontineId: string, arriereId: string, reglementId: string) {
    return this.http.delete<ArriereMembre[]>(
      `${this.api}/${tontineId}/arrieres/${arriereId}/reglements/${reglementId}`);
  }

  enregistrerVersementsAnterieurs(tontineId: string, req: VersementsAnterieursRequest) {
    return this.http.put<VersementsAnterieursResponse>(
      `${this.api}/${tontineId}/versements-anterieurs`, req);
  }
}
