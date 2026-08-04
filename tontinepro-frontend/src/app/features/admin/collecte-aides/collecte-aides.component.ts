import { Component, OnInit, signal, computed, inject, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { AideService } from '../../../core/services/aide.service';
import { FondsAideService } from '../../../core/services/fonds-aide.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';
import { CollecteAidesResponse } from '../../../core/models/aide.model';

@Component({
  selector: 'app-collecte-aides',
  imports: [RouterLink, DecimalPipe, DatePipe],
  templateUrl: './collecte-aides.component.html',
})
export class CollecteAidesComponent implements OnInit {
  private svc = inject(AideService);
  private fondsSvc = inject(FondsAideService);
  private ctx = inject(TontineContextService);

  private tontineId = signal('');

  data    = signal<CollecteAidesResponse | null>(null);
  loading = signal(true);
  error   = signal('');
  paiementId = signal<string | null>(null);

  nbAides = computed(() => this.data()?.aides.length ?? 0);

  constructor() {
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => { this.tontineId.set(id); this.charger(id); });
    });
  }

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) this.loading.set(false);
  }

  private charger(tontineId: string): void {
    this.loading.set(true);
    this.error.set('');
    this.svc.getCollecte(tontineId).subscribe({
      next: d => { this.data.set(d); this.loading.set(false); },
      error: e => { this.error.set(this.messageErreur(e, 'Erreur de chargement')); this.loading.set(false); },
    });
  }

  payer(contributionId: string): void {
    this.paiementId.set(contributionId);
    this.fondsSvc.payerContribution(contributionId).subscribe({
      next: () => { this.paiementId.set(null); this.charger(this.tontineId()); },
      error: e => { this.error.set(this.messageErreur(e, 'Paiement impossible')); this.paiementId.set(null); },
    });
  }

  /**
   * Un 403 signifie ici « vous n'êtes pas celui qui encaisse », jamais un
   * incident technique. Le dire explicitement : « Paiement impossible » laissait
   * croire à une panne alors que la règle est simplement que l'encaissement
   * revient au trésorier.
   */
  private messageErreur(e: { status?: number; error?: { detail?: string; message?: string } }, defaut: string): string {
    if (e.status === 403) {
      return e.error?.detail
        ?? "L'encaissement des contributions revient au Trésorier de la tontine.";
    }
    return e.error?.detail ?? e.error?.message ?? defaut;
  }

  progression(collecte: number, objectif: number): number {
    if (!objectif) return 0;
    return Math.min(100, Math.round((collecte / objectif) * 100));
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
}
