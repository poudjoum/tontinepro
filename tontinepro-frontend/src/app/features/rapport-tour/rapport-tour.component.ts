import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { SessionService } from '../../core/services/session.service';
import { RapportTourResponse } from '../../core/models/session.model';

@Component({
  selector: 'app-rapport-tour',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './rapport-tour.component.html',
})
export class RapportTourComponent implements OnInit {
  private route      = inject(ActivatedRoute);
  private sessionSvc = inject(SessionService);

  rapport  = signal<RapportTourResponse | null>(null);
  loading  = signal(true);
  error    = signal('');
  today    = new Date().toISOString();

  private MOIS = ['', 'Janvier', 'Février', 'Mars', 'Avril', 'Mai',
    'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

  ngOnInit(): void {
    const sessionId           = this.route.snapshot.paramMap.get('sessionId')!;
    const ordreBeneficiaireId = this.route.snapshot.paramMap.get('ordreBeneficiaireId')!;
    this.sessionSvc.getRapportTour(sessionId, ordreBeneficiaireId).subscribe({
      next:  r  => { this.rapport.set(r); this.loading.set(false); },
      error: e  => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur chargement'); this.loading.set(false); },
    });
  }

  moisLabel(n: number): string { return this.MOIS[n] ?? ''; }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  formatDate(d: string | null | undefined): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  imprimer(): void { window.print(); }
}
