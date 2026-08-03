import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AideService } from '../../../core/services/aide.service';
import { FondsAideService } from '../../../core/services/fonds-aide.service';
import { AideSuiviResponse } from '../../../core/models/aide.model';

@Component({
  selector: 'app-aide-suivi',
  imports: [RouterLink],
  templateUrl: './aide-suivi.component.html',
})
export class AideSuiviComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private svc = inject(AideService);
  private fondsSvc = inject(FondsAideService);

  private aideId = '';

  suivi   = signal<AideSuiviResponse | null>(null);
  loading = signal(true);
  error   = signal('');
  paiementId = signal<string | null>(null);

  progression = computed(() => {
    const s = this.suivi();
    if (!s || s.totalAttendu <= 0) return 0;
    return Math.min(100, Math.round((s.totalCollecte / s.totalAttendu) * 100));
  });

  ngOnInit(): void {
    this.aideId = this.route.snapshot.paramMap.get('id')!;
    this.charger();
  }

  private charger(): void {
    this.loading.set(true);
    this.svc.getSuivi(this.aideId).subscribe({
      next: s => { this.suivi.set(s); this.loading.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur de chargement'); this.loading.set(false); },
    });
  }

  payer(contributionId: string): void {
    this.paiementId.set(contributionId);
    this.fondsSvc.payerContribution(contributionId).subscribe({
      next: () => { this.paiementId.set(null); this.charger(); },
      error: e => { this.error.set(e.error?.detail ?? 'Paiement impossible'); this.paiementId.set(null); },
    });
  }

  statutLabel(s: string): string {
    return s === 'PAYEE' ? 'Versée' : s === 'VALIDEE' ? 'Approuvée' : s === 'SOUMISE' ? 'En attente' : s;
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  formatDate(d: string | null | undefined): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
