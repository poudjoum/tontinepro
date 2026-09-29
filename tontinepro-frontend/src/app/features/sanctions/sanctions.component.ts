import { Component, OnInit, signal, computed, inject, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SanctionService } from '../../core/services/sanction.service';
import { TontineContextService } from '../../core/services/tontine-context.service';
import { SanctionResponse, TypeSanction } from '../../core/models/sanction.model';

@Component({
  selector: 'app-sanctions',
  imports: [RouterLink],
  templateUrl: './sanctions.component.html',
})
export class SanctionsComponent implements OnInit {
  private svc = inject(SanctionService);
  private ctx = inject(TontineContextService);

  constructor() {
    // Mes sanctions dans la tontine affichée, rechargées quand on en change.
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => this.charger(id));
    });
  }

  sanctions    = signal<SanctionResponse[]>([]);
  loading      = signal(true);
  error        = signal('');

  nonReglees   = computed(() => this.sanctions().filter(s => !s.payee));
  reglees      = computed(() => this.sanctions().filter(s => s.payee));
  totalDu      = computed(() =>
    this.nonReglees().reduce((s, x) => s + x.montant, 0));

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) this.loading.set(false);
  }

  private charger(tontineId: string): void {
    this.loading.set(true);
    this.svc.mesSanctions(tontineId).subscribe({
      next: s => { this.sanctions.set(s); this.loading.set(false); },
      error: () => { this.error.set('Impossible de charger vos sanctions'); this.loading.set(false); },
    });
  }

  fcfa(n: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  labelType(t: TypeSanction): string {
    switch (t) {
      case 'RETARD_COTISATION': return 'Retard de cotisation';
      case 'ABSENCE_REUNION':  return 'Absence à une réunion';
      default:                 return 'Autre';
    }
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR');
  }
}
