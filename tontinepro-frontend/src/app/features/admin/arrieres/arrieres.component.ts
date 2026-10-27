import { Component, OnInit, signal, computed, inject, effect, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Observable } from 'rxjs';
import { FondsAideService } from '../../../core/services/fonds-aide.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';
import { ArriereMembre } from '../../../core/models/aide.model';
import { MembreResponse } from '../../../core/models/membre.model';

/**
 * Arriérés : fonds non cotisé par un membre avant l'arrivée de la tontine dans
 * l'application. Chaque règlement encaissé crédite le fonds d'aide.
 */
@Component({
  selector: 'app-arrieres',
  imports: [FormsModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './arrieres.component.html',
})
export class ArrieresComponent implements OnInit {
  private svc = inject(FondsAideService);
  private membreSvc = inject(MembreService);
  private ctx = inject(TontineContextService);

  arrieres = signal<ArriereMembre[]>([]);
  membres  = signal<MembreResponse[]>([]);
  loading  = signal(true);
  saving   = signal(false);
  error    = signal('');
  success  = signal('');

  private tontineId = signal('');

  totalResteDu = computed(() => this.arrieres().reduce((s, a) => s + a.resteDu, 0));

  // Nouvel arriéré
  formOuvert = signal(false);
  nouveauMembreId = '';
  nouveauLibelle = 'Fonds non cotisé';
  nouveauMontant: number | null = null;
  nouvelleDate = new Date().toISOString().slice(0, 10);

  // Règlement en cours de saisie
  reglementId = signal<string | null>(null);
  reglementMontant: number | null = null;
  reglementDate = new Date().toISOString().slice(0, 10);

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

  private charger(): void {
    this.loading.set(true);
    this.error.set('');
    this.membreSvc.getAll(this.tontineId(), 'ACTIF').subscribe({
      next: m => this.membres.set([...m].sort((a, b) => a.prenom.localeCompare(b.prenom, 'fr'))),
    });
    this.svc.getArrieres(this.tontineId()).subscribe({
      next: a => { this.arrieres.set(a); this.loading.set(false); },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Impossible de charger les arriérés.');
        this.loading.set(false);
      },
    });
  }

  creer(): void {
    const montant = Number(this.nouveauMontant);
    if (!this.nouveauMembreId) { this.error.set('Choisissez le membre.'); return; }
    if (!montant || montant <= 0) { this.error.set('Saisissez le montant de l\'arriéré.'); return; }
    const m = this.membres().find(x => x.id === this.nouveauMembreId);
    this.executer(this.svc.creerArriere(this.tontineId(), {
      membreId: this.nouveauMembreId, libelle: this.nouveauLibelle || null, montant,
      dateConstat: this.nouvelleDate || null,
    }), `Arriéré de ${this.fcfa(montant)} enregistré pour ${m?.prenom} ${m?.nom}.`, () => {
      this.formOuvert.set(false);
      this.nouveauMembreId = '';
      this.nouveauMontant = null;
      this.nouveauLibelle = 'Fonds non cotisé';
    });
  }

  supprimer(a: ArriereMembre): void {
    if (!confirm(`Supprimer l'arriéré de ${a.prenom} ${a.nom} (${this.fcfa(a.montant)}) ?`)) return;
    this.executer(this.svc.supprimerArriere(this.tontineId(), a.id), 'Arriéré supprimé.');
  }

  ouvrirReglement(a: ArriereMembre): void {
    this.reglementMontant = a.resteDu;
    this.reglementDate = new Date().toISOString().slice(0, 10);
    this.reglementId.set(a.id);
  }

  regler(a: ArriereMembre): void {
    const montant = Number(this.reglementMontant);
    if (!montant || montant <= 0) { this.error.set('Saisissez le montant réglé.'); return; }
    this.executer(this.svc.reglerArriere(this.tontineId(), a.id, montant, this.reglementDate || null),
      `${this.fcfa(montant)} encaissés de ${a.prenom} ${a.nom} — versés au fonds d'aide.`,
      () => this.reglementId.set(null));
  }

  annulerReglement(a: ArriereMembre, r: { id: string; montant: number }): void {
    if (!confirm(`Annuler le règlement de ${this.fcfa(r.montant)} de ${a.prenom} ${a.nom} ?`)) return;
    this.executer(this.svc.annulerReglementArriere(this.tontineId(), a.id, r.id), 'Règlement annulé.');
  }

  private executer(appel: Observable<ArriereMembre[]>, message: string, apres?: () => void): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    appel.subscribe({
      next: a => { this.arrieres.set(a); this.saving.set(false); this.success.set(message); apres?.(); },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Opération impossible.');
        this.saving.set(false);
      },
    });
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
}
