import { Component, OnInit, signal, inject, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MembreService } from '../../core/services/membre.service';
import { TontineContextService } from '../../core/services/tontine-context.service';
import { SuiviMois } from '../../core/models/suivi.model';

const MOIS_FR = ['', 'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
                 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

@Component({
  selector: 'app-suivi',
  imports: [RouterLink, FormsModule],
  templateUrl: './suivi.component.html',
})
export class SuiviComponent implements OnInit {
  private svc = inject(MembreService);
  private ctx = inject(TontineContextService);

  constructor() {
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => this.load());
    });
  }

  annee = signal(new Date().getFullYear());
  suivi = signal<SuiviMois[]>([]);
  loading = signal(true);
  error = signal('');
  expanded = signal<Set<number>>(new Set());

  annees = Array.from({ length: 3 }, (_, i) => new Date().getFullYear() - i);

  ngOnInit(): void {
    this.ctx.init();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.svc.getSuivi(this.annee(), this.ctx.tontineCouranteId() ?? undefined).subscribe({
      next: d => { this.suivi.set(d); this.loading.set(false); },
      error: () => { this.error.set('Impossible de charger le suivi'); this.loading.set(false); },
    });
  }

  changeAnnee(a: number): void {
    this.annee.set(a);
    this.expanded.set(new Set());
    this.load();
  }

  toggle(mois: number): void {
    const s = new Set(this.expanded());
    if (s.has(mois)) s.delete(mois); else s.add(mois);
    this.expanded.set(s);
  }

  isExpanded(mois: number): boolean {
    return this.expanded().has(mois);
  }

  moisNom(m: number): string { return MOIS_FR[m] ?? String(m); }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  statutCotisation(s: string): string {
    return s === 'PAYEE' ? 'Payée' : s === 'EN_RETARD' ? 'En retard' : 'En attente';
  }

  badgeCotisation(s: string): string {
    return s === 'PAYEE' ? 'badge-success' : s === 'EN_RETARD' ? 'badge-danger' : 'badge-warning';
  }

  statutEcheance(s: string): string {
    return s === 'PAYEE' ? 'Payée' : s === 'EN_RETARD' ? 'En retard' : 'À payer';
  }

  badgeEcheance(s: string): string {
    return s === 'PAYEE' ? 'badge-success' : s === 'EN_RETARD' ? 'badge-danger' : 'badge-warning';
  }

  libelleMouvement(t: string): string {
    return t === 'DEPOT' ? 'Dépôt' : t === 'RETRAIT' ? 'Retrait' : 'Intérêts';
  }

  signeMouvement(t: string): string {
    return t === 'RETRAIT' ? '−' : '+';
  }

  colorMouvement(t: string): string {
    return t === 'RETRAIT' ? 'text-red-600' : 'text-green-600';
  }

  libelleSanction(t: string): string {
    const map: Record<string, string> = {
      RETARD_COTISATION: 'Retard de cotisation',
      ABSENCE_REUNION: 'Absence à une réunion',
      AUTRE: 'Autre',
    };
    return map[t] ?? t;
  }

  hasData(m: SuiviMois): boolean {
    return m.cotisations.length + m.contributions.length + m.sanctions.length
         + m.echeancesPret.length + m.mouvementsEpargne.length > 0;
  }
}
