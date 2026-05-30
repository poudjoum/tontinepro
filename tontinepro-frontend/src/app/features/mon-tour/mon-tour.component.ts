import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SessionService } from '../../core/services/session.service';
import { CotisationService } from '../../core/services/cotisation.service';
import { CotisationResponse } from '../../core/models/cotisation.model';
import { MonTourResponse, MonBeneficeResponse, OrdreBeneficiaireResponse } from '../../core/models/session.model';

@Component({
  selector: 'app-mon-tour',
  imports: [RouterLink],
  templateUrl: './mon-tour.component.html',
})
export class MonTourComponent implements OnInit {
  private svc    = inject(SessionService);
  private cotSvc = inject(CotisationService);

  tour          = signal<MonTourResponse | null>(null);
  benefices     = signal<MonBeneficeResponse[]>([]);
  echeancier    = signal<OrdreBeneficiaireResponse[]>([]);
  mesCotisations = signal<CotisationResponse[]>([]);
  loading       = signal(true);
  error         = signal('');

  echeancierTrie = computed(() =>
    [...this.echeancier()].sort((a, b) => {
      if (!a.dateBenefice) return 1;
      if (!b.dateBenefice) return -1;
      return a.dateBenefice.localeCompare(b.dateBenefice);
    })
  );

  progressPct = computed(() => {
    const t = this.tour();
    if (!t || t.totalMembres === 0) return 0;
    if (t.beneficie) return 100;
    return Math.round(((t.ordre - 1) / t.totalMembres) * 100);
  });

  ngOnInit(): void {
    let pending = 3;
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

    this.cotSvc.getMesCotisations().subscribe({
      next: c => { this.mesCotisations.set(c); done(); },
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

  montantLigne(ligne: OrdreBeneficiaireResponse, estMoi: boolean): number | null {
    if (estMoi && ligne.beneficie && ligne.montantRecu != null) return ligne.montantRecu;
    if (!ligne.dateBenefice) return 0;
    const d = new Date(ligne.dateBenefice);
    const mois  = d.getMonth() + 1;
    const annee = d.getFullYear();
    const cot = this.mesCotisations().find(c => c.mois === mois && c.annee === annee);
    return cot?.statut === 'PAYEE' ? cot.montant : 0;
  }

  joursRestants(d: string | null | undefined): string | null {
    if (!d) return null;
    const diff = Math.ceil((new Date(d).getTime() - Date.now()) / 86400000);
    if (diff < 0) return 'Date passée';
    if (diff === 0) return "Aujourd'hui !";
    return `Dans ${diff} jour${diff > 1 ? 's' : ''}`;
  }
}
