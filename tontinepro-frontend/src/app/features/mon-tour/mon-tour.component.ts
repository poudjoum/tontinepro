import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SessionService } from '../../core/services/session.service';
import { MonTourResponse, MonBeneficeResponse, OrdreBeneficiaireResponse } from '../../core/models/session.model';

@Component({
  selector: 'app-mon-tour',
  imports: [RouterLink],
  templateUrl: './mon-tour.component.html',
})
export class MonTourComponent implements OnInit {
  private svc = inject(SessionService);

  tour       = signal<MonTourResponse | null>(null);
  benefices  = signal<MonBeneficeResponse[]>([]);
  echeancier = signal<OrdreBeneficiaireResponse[]>([]);
  loading    = signal(true);
  error      = signal('');

  progressPct = computed(() => {
    const t = this.tour();
    if (!t || t.totalMembres === 0) return 0;
    if (t.beneficie) return 100;
    return Math.round(((t.ordre - 1) / t.totalMembres) * 100);
  });

  ngOnInit(): void {
    let pending = 2;
    const done = () => { if (--pending === 0) this.loading.set(false); };

    this.svc.monTour().subscribe({
      next: t => {
        this.tour.set(t);
        this.svc.echeancier(t.sessionId).subscribe({
          next: e => this.echeancier.set(e),
          error: () => {},
        });
        done();
      },
      error: () => { done(); },
    });

    this.svc.mesBenefices().subscribe({
      next: b => { this.benefices.set(b); done(); },
      error: () => { done(); },
    });
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  formatDate(d: string | null | undefined): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  joursRestants(d: string | null | undefined): string | null {
    if (!d) return null;
    const diff = Math.ceil((new Date(d).getTime() - Date.now()) / 86400000);
    if (diff < 0) return 'Date passée';
    if (diff === 0) return "Aujourd'hui !";
    return `Dans ${diff} jour${diff > 1 ? 's' : ''}`;
  }
}
