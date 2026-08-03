import { Component, OnInit, signal, inject, effect, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RubriqueAideService } from '../../../core/services/rubrique-aide.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';
import {
  RubriqueAideResponse,
  RubriqueAideRequest,
  ModeCalculAide,
  MODE_CALCUL_AIDE_LABELS,
  PorteeLimiteAide,
  PORTEE_LIMITE_LABELS,
  TypeAide,
  TYPES_AIDE,
  TYPE_AIDE_LABELS,
} from '../../../core/models/aide.model';

@Component({
  selector: 'app-bareme-aide',
  imports: [FormsModule, RouterLink],
  templateUrl: './bareme-aide.component.html',
})
export class BaremeAideComponent implements OnInit {
  private svc = inject(RubriqueAideService);
  private ctx = inject(TontineContextService);

  rubriques = signal<RubriqueAideResponse[]>([]);
  loading   = signal(true);
  saving    = signal(false);
  error     = signal('');
  success   = signal('');

  private tontineId = signal('');

  // Formulaire (création ou édition)
  editId = signal<string | null>(null);
  form: RubriqueAideRequest = this.formVide();

  // Suppression à confirmer
  confirmSuppr = signal<string | null>(null);

  readonly types: TypeAide[] = TYPES_AIDE;
  readonly typeLabels = TYPE_AIDE_LABELS;
  readonly modes: ModeCalculAide[] = ['PAR_PERSONNE', 'FORFAITAIRE'];
  readonly modeLabels = MODE_CALCUL_AIDE_LABELS;
  readonly portees: PorteeLimiteAide[] = ['VIE', 'SESSION', 'ANNEE'];
  readonly porteeLabels = PORTEE_LIMITE_LABELS;

  constructor() {
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => { this.tontineId.set(id); this.charger(id); });
    });
  }

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) this.loading.set(false);
  }

  private formVide(): RubriqueAideRequest {
    return {
      libelle: '',
      typeAide: 'MALADIE',
      modeCalcul: 'PAR_PERSONNE',
      montantReference: 0,
      prefinancable: true,
      actif: true,
      description: '',
      limiteParBeneficiaire: null,
      porteeLimite: 'VIE',
      variantes: '',
    };
  }

  private charger(tontineId: string): void {
    this.loading.set(true);
    this.svc.lister(tontineId, false).subscribe({
      next: list => { this.rubriques.set(list); this.loading.set(false); },
      error: () => { this.error.set('Impossible de charger le barème.'); this.loading.set(false); },
    });
  }

  nouveau(): void {
    this.editId.set(null);
    this.form = this.formVide();
    this.success.set('');
    this.error.set('');
  }

  editer(r: RubriqueAideResponse): void {
    this.editId.set(r.id);
    this.form = {
      libelle: r.libelle,
      typeAide: r.typeAide,
      modeCalcul: r.modeCalcul,
      montantReference: r.montantReference,
      prefinancable: r.prefinancable,
      actif: r.actif,
      description: r.description ?? '',
      limiteParBeneficiaire: r.limiteParBeneficiaire,
      porteeLimite: r.porteeLimite,
      variantes: r.variantes ?? '',
    };
    this.success.set('');
    this.error.set('');
  }

  enregistrer(): void {
    if (!this.form.libelle?.trim()) { this.error.set('Le libellé est obligatoire.'); return; }
    if (!this.form.montantReference || this.form.montantReference <= 0) {
      this.error.set('Le montant de référence doit être positif.'); return;
    }
    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    const id = this.editId();
    const obs = id
      ? this.svc.modifier(id, this.form)
      : this.svc.creer(this.tontineId(), this.form);

    obs.subscribe({
      next: () => {
        this.saving.set(false);
        this.success.set(id ? 'Rubrique mise à jour.' : 'Rubrique créée.');
        this.nouveau();
        this.charger(this.tontineId());
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de l\'enregistrement.');
        this.saving.set(false);
      },
    });
  }

  demanderSuppression(id: string): void { this.confirmSuppr.set(id); }
  annulerSuppression(): void { this.confirmSuppr.set(null); }

  supprimer(id: string): void {
    this.saving.set(true);
    this.svc.supprimer(id).subscribe({
      next: () => {
        this.saving.set(false);
        this.confirmSuppr.set(null);
        if (this.editId() === id) this.nouveau();
        this.charger(this.tontineId());
      },
      error: e => {
        this.error.set(e.error?.detail ?? 'Suppression impossible.');
        this.saving.set(false);
        this.confirmSuppr.set(null);
      },
    });
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
}
