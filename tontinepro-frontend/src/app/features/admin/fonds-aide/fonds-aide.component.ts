import { Component, OnInit, signal, computed, inject, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { SessionService } from '../../../core/services/session.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';
import { FondsAideMensuelResponse } from '../../../core/models/session.model';

@Component({
  selector: 'app-fonds-aide',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './fonds-aide.component.html',
})
export class FondsAideComponent implements OnInit {
  private sessionSvc = inject(SessionService);
  private ctx        = inject(TontineContextService);

  data     = signal<FondsAideMensuelResponse | null>(null);
  loading  = signal(true);
  error    = signal('');
  today    = new Date().toISOString();

  /** Aucune session en cours pour la tontine courante. */
  aucuneSession = signal(false);

  nbMois = computed(() => this.data()?.mois.length ?? 0);

  private MOIS_COURT = ['', 'Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin',
    'Juil', 'Août', 'Sept', 'Oct', 'Nov', 'Déc'];

  constructor() {
    // Suit la tontine courante : recharge à chaque changement de sélection.
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => this.charger(id));
    });
  }

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) this.loading.set(false);
  }

  private charger(tontineId: string): void {
    this.loading.set(true);
    this.error.set('');
    this.aucuneSession.set(false);
    this.data.set(null);

    this.sessionSvc.listerSessions(tontineId).subscribe({
      next: list => {
        const enCours = list.find(s => s.statut === 'EN_COURS');
        if (!enCours) { this.aucuneSession.set(true); this.loading.set(false); return; }
        this.sessionSvc.getFondsAideMensuel(enCours.id).subscribe({
          next: d  => { this.data.set(d); this.loading.set(false); },
          error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur de chargement'); this.loading.set(false); },
        });
      },
      error: () => { this.error.set('Impossible de charger les sessions.'); this.loading.set(false); },
    });
  }

  moisLabel(mois: number, annee: number): string {
    return `${this.MOIS_COURT[mois] ?? ''} ${String(annee).slice(-2)}`;
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  /** Impression en paysage (matrice large) sans impacter les autres rapports en portrait. */
  imprimer(): void {
    const STYLE_ID = 'print-landscape-fonds-aide';
    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.media = 'print';
    style.textContent = '@page { size: A4 landscape; margin: 10mm; }';
    document.head.appendChild(style);

    const cleanup = () => {
      document.getElementById(STYLE_ID)?.remove();
      window.removeEventListener('afterprint', cleanup);
    };
    window.addEventListener('afterprint', cleanup);
    // Filet de sécurité si l'événement afterprint n'est pas émis (certains navigateurs).
    setTimeout(cleanup, 1000);

    window.print();
  }
}
