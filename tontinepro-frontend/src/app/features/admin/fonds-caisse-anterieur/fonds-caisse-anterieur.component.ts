import { Component, OnInit, signal, computed, inject, effect, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FondsAideService } from '../../../core/services/fonds-aide.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';
import { LigneVersementAnterieur } from '../../../core/models/aide.model';

/**
 * Fond de caisse versé avant l'arrivée de la tontine dans l'application.
 * Le montant déclaré par membre est compté comme déjà payé : il réduit la
 * retenue fond de caisse au bénéfice et crédite le solde du fonds.
 */
@Component({
  selector: 'app-fonds-caisse-anterieur',
  imports: [FormsModule, RouterLink],
  templateUrl: './fonds-caisse-anterieur.component.html',
})
export class FondsCaisseAnterieurComponent implements OnInit {
  private svc = inject(FondsAideService);
  private ctx = inject(TontineContextService);

  readonly anneeCourante = new Date().getFullYear();
  annee = signal(this.anneeCourante);

  lignes      = signal<LigneVersementAnterieur[]>([]);
  obligation  = signal(0);
  /** Montants saisis, par membreId. */
  saisie      = signal<Record<string, number | null>>({});

  loading = signal(true);
  saving  = signal(false);
  error   = signal('');
  success = signal('');

  private tontineId = signal('');

  total = computed(() =>
    Object.values(this.saisie()).reduce<number>((s, v) => s + (v ?? 0), 0));

  /** Lignes dont le montant saisi diffère de la déclaration enregistrée. */
  modifiees = computed(() => {
    const s = this.saisie();
    return this.lignes().filter(l => (s[l.membreId] ?? 0) !== l.montant);
  });

  constructor() {
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => { this.tontineId.set(id); this.charger(); });
    });
  }

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) this.loading.set(false);
  }

  changerAnnee(delta: number): void {
    this.annee.update(a => a + delta);
    this.success.set('');
    this.charger();
  }

  private charger(): void {
    this.loading.set(true);
    this.error.set('');
    this.svc.getVersementsAnterieurs(this.tontineId(), this.annee()).subscribe({
      next: r => {
        this.lignes.set(r.lignes);
        this.obligation.set(r.obligationAnnuelle);
        this.saisie.set(Object.fromEntries(r.lignes.map(l => [l.membreId, l.montant || null])));
        this.loading.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Impossible de charger les membres.');
        this.loading.set(false);
      },
    });
  }

  setMontant(membreId: string, v: number | null): void {
    this.saisie.update(s => ({ ...s, [membreId]: v }));
    this.success.set('');
  }

  /** Tous les membres ont versé le fond annuel complet. */
  toutAuMontantAnnuel(): void {
    const o = this.obligation();
    if (!o) return;
    this.saisie.set(Object.fromEntries(this.lignes().map(l => [l.membreId, o])));
    this.success.set('');
  }

  enregistrer(): void {
    const s = this.saisie();
    const invalide = this.modifiees().find(l => (s[l.membreId] ?? 0) < 0);
    if (invalide) { this.error.set(`Montant négatif pour ${invalide.nomPrenom}.`); return; }

    const lignes = this.modifiees().map(l => ({ membreId: l.membreId, montant: s[l.membreId] ?? 0 }));
    if (!lignes.length) return;

    this.saving.set(true);
    this.error.set('');
    this.svc.enregistrerVersementsAnterieurs(this.tontineId(), { annee: this.annee(), lignes }).subscribe({
      next: r => {
        this.lignes.set(r.lignes);
        this.saisie.set(Object.fromEntries(r.lignes.map(l => [l.membreId, l.montant || null])));
        this.saving.set(false);
        this.success.set(`${lignes.length} membre(s) mis à jour — total déclaré ${this.fcfa(r.total)}.`);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de l\'enregistrement.');
        this.saving.set(false);
      },
    });
  }

  resteDu(membreId: string): number {
    return Math.max(0, this.obligation() - (this.saisie()[membreId] ?? 0));
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
}
