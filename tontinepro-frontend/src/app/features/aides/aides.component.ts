import { Component, OnInit, signal, inject, effect, untracked } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { AideService } from '../../core/services/aide.service';
import { RubriqueAideService } from '../../core/services/rubrique-aide.service';
import { TontineContextService } from '../../core/services/tontine-context.service';
import {
  AideResponse, TYPE_AIDE_LABELS, TYPES_AIDE, StatutAide,
  RubriqueAideResponse, SimulationAideResponse,
} from '../../core/models/aide.model';

const STATUT_BADGE: Record<string, string> = {
  SOUMISE: 'badge-warning', VALIDEE: 'badge-info', REJETEE: 'badge-danger', PAYEE: 'badge-success',
};
const STATUT_LABEL: Record<string, string> = {
  SOUMISE: 'En attente', VALIDEE: 'Approuvée', REJETEE: 'Rejetée', PAYEE: 'Versée',
};

@Component({
  selector: 'app-aides',
  templateUrl: './aides.component.html',
})
export class AidesComponent implements OnInit {
  auth = inject(AuthService);
  private svc = inject(AideService);
  private rubriqueSvc = inject(RubriqueAideService);
  private ctx = inject(TontineContextService);

  constructor() {
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => this.charger());
    });
  }

  aides      = signal<AideResponse[]>([]);
  loading    = signal(true);
  submitting = signal(false);
  error      = signal('');

  // Membre — formulaire demande (barème)
  showForm   = signal(false);
  motif      = signal('');
  rubriques  = signal<RubriqueAideResponse[]>([]);
  rubriqueId = signal<string>('');
  simulation = signal<SimulationAideResponse | null>(null);
  simLoading = signal(false);

  // Admin — filtres + actions
  filtre     = signal<StatutAide | ''>('');
  actionId   = signal<string | null>(null);
  actionType = signal<'valider' | 'rejeter' | null>(null);
  montantAccorde = signal(0);
  motifRejet     = signal('');

  // Admin — activation d'une aide du barème
  activationId = signal<string | null>(null);
  prefinancer  = signal(false);

  readonly TYPES_AIDE = TYPES_AIDE;
  readonly TYPE_AIDE_LABELS = TYPE_AIDE_LABELS;

  ngOnInit(): void { this.ctx.init(); }

  charger(): void {
    this.loading.set(true);
    const tontineId = this.ctx.tontineCouranteId() ?? undefined;
    const obs = this.auth.isAdmin()
      ? this.svc.getAll(this.filtre() || undefined)
      : this.svc.getMesDemandes(tontineId);
    obs.subscribe({
      next:  data => { this.aides.set(data); this.loading.set(false); },
      error: e    => { this.error.set(e.message ?? 'Erreur'); this.loading.set(false); },
    });
    // Barème des rubriques actives (pour la demande membre)
    if (!this.auth.isAdmin() && tontineId) {
      this.rubriqueSvc.lister(tontineId, true).subscribe({
        next: list => this.rubriques.set(list),
        error: () => this.rubriques.set([]),
      });
    }
  }

  filtrer(f: StatutAide | ''): void { this.filtre.set(f); this.charger(); }

  selectionnerRubrique(id: string): void {
    this.rubriqueId.set(id);
    this.simulation.set(null);
    if (!id) return;
    this.simLoading.set(true);
    this.rubriqueSvc.simuler(id).subscribe({
      next: s => { this.simulation.set(s); this.simLoading.set(false); },
      error: () => { this.simLoading.set(false); this.error.set('Calcul du montant impossible.'); },
    });
  }

  soumettreRubrique(): void {
    if (!this.rubriqueId() || !this.motif().trim() || this.submitting()) return;
    this.submitting.set(true);
    this.svc.soumettreDepuisRubrique(this.rubriqueId(), this.motif()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showForm.set(false);
        this.rubriqueId.set('');
        this.simulation.set(null);
        this.motif.set('');
        this.charger();
      },
      error: e => { this.error.set(e.error?.detail ?? e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  ouvrirAction(id: string, type: 'valider' | 'rejeter'): void {
    this.actionId.set(id);
    this.actionType.set(type);
    this.montantAccorde.set(0);
    this.motifRejet.set('');
  }

  confirmerAction(): void {
    const id = this.actionId();
    const type = this.actionType();
    if (!id || !type || this.submitting()) return;
    this.submitting.set(true);

    const obs = type === 'valider'
      ? this.svc.valider(id, this.montantAccorde())
      : this.svc.rejeter(id, this.motifRejet());

    obs.subscribe({
      next: u => {
        this.aides.update(l => l.map(a => a.id === id ? u : a));
        this.actionId.set(null); this.actionType.set(null);
        this.submitting.set(false);
      },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  marquerPayee(id: string): void {
    this.submitting.set(true);
    this.svc.marquerPayee(id).subscribe({
      next: u => { this.aides.update(l => l.map(a => a.id === id ? u : a)); this.submitting.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  ouvrirActivation(id: string): void {
    this.activationId.set(id);
    this.prefinancer.set(false);
  }

  confirmerActivation(): void {
    const id = this.activationId();
    if (!id || this.submitting()) return;
    this.submitting.set(true);
    this.svc.activer(id, this.prefinancer()).subscribe({
      next: u => {
        this.aides.update(l => l.map(a => a.id === id ? u : a));
        this.activationId.set(null);
        this.submitting.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  verser(id: string): void {
    this.submitting.set(true);
    this.svc.verser(id).subscribe({
      next: u => { this.aides.update(l => l.map(a => a.id === id ? u : a)); this.submitting.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  fcfa(n: number | null): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
  statutLabel(s: string): string { return STATUT_LABEL[s] ?? s; }
  statutBadge(s: string): string { return STATUT_BADGE[s] ?? 'badge-gray'; }
  formatDate(d: string | null): string { return d ? new Date(d).toLocaleDateString('fr-FR') : '—'; }
}
