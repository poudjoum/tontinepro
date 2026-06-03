import { Component, OnInit, signal, inject, effect, untracked } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { AideService } from '../../core/services/aide.service';
import { TontineContextService } from '../../core/services/tontine-context.service';
import { AideResponse, TypeAide, TYPE_AIDE_LABELS, TYPES_AIDE, StatutAide } from '../../core/models/aide.model';

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

  // Membre — formulaire demande
  showForm   = signal(false);
  typeAide   = signal<TypeAide>('MALADIE');
  montant    = signal(0);
  motif      = signal('');

  // Admin — filtres + actions
  filtre     = signal<StatutAide | ''>('');
  actionId   = signal<string | null>(null);
  actionType = signal<'valider' | 'rejeter' | null>(null);
  montantAccorde = signal(0);
  motifRejet     = signal('');

  readonly TYPES_AIDE = TYPES_AIDE;
  readonly TYPE_AIDE_LABELS = TYPE_AIDE_LABELS;

  ngOnInit(): void { this.ctx.init(); }

  charger(): void {
    this.loading.set(true);
    const obs = this.auth.isAdmin()
      ? this.svc.getAll(this.filtre() || undefined)
      : this.svc.getMesDemandes(this.ctx.tontineCouranteId() ?? undefined);
    obs.subscribe({
      next:  data => { this.aides.set(data); this.loading.set(false); },
      error: e    => { this.error.set(e.message ?? 'Erreur'); this.loading.set(false); },
    });
  }

  filtrer(f: StatutAide | ''): void { this.filtre.set(f); this.charger(); }

  soumettre(): void {
    if (!this.motif().trim() || this.montant() <= 0 || this.submitting()) return;
    this.submitting.set(true);
    this.svc.soumettre(this.typeAide(), this.montant(), this.motif()).subscribe({
      next: () => { this.submitting.set(false); this.showForm.set(false); this.montant.set(0); this.motif.set(''); this.charger(); },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
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
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
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
