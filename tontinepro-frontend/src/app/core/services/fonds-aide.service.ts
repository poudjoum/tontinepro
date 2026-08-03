import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

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
}
