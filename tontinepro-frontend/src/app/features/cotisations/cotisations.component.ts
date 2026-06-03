import { Component, OnInit, signal, inject, computed, effect, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { CotisationService } from '../../core/services/cotisation.service';
import { TontineContextService } from '../../core/services/tontine-context.service';
import { CotisationResponse } from '../../core/models/cotisation.model';

const MOIS = ['', 'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
              'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

@Component({
  selector: 'app-cotisations',
  imports: [FormsModule],
  templateUrl: './cotisations.component.html',
})
export class CotisationsComponent implements OnInit {
  auth = inject(AuthService);
  private svc = inject(CotisationService);
  private ctx = inject(TontineContextService);

  constructor() {
    // Recharge dès que la tontine courante change (sélecteur du header).
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => this.charger());
    });
  }

  cotisations  = signal<CotisationResponse[]>([]);
  loading      = signal(true);
  error        = signal('');
  submitting   = signal(false);
  paiementId   = signal<string | null>(null);
  paiementRef  = signal('');
  paiementMontant  = signal(0);
  paiementFondAide = signal(0);

  mois  = signal(new Date().getMonth() + 1);
  annee = signal(new Date().getFullYear());

  payees    = computed(() => this.cotisations().filter(c => c.statut === 'PAYEE').length);
  enRetard  = computed(() => this.cotisations().filter(c => c.statut === 'EN_RETARD').length);
  enAttente = computed(() => this.cotisations().filter(c => c.statut === 'EN_ATTENTE').length);
  montantCollecte = computed(() =>
    this.cotisations().filter(c => c.statut === 'PAYEE').reduce((s, c) => s + c.montant, 0)
  );

  ngOnInit(): void { this.ctx.init(); }

  charger(): void {
    this.loading.set(true);
    this.error.set('');
    const tontineId = this.ctx.tontineCouranteId() ?? undefined;
    const obs = this.auth.isGestionnaire()
      ? this.svc.getAll(this.mois(), this.annee(), undefined, tontineId)
      : this.svc.getMesCotisations(tontineId);
    obs.subscribe({
      next:  data => { this.cotisations.set(data); this.loading.set(false); },
      error: e    => { this.error.set(e.message ?? 'Erreur'); this.loading.set(false); },
    });
  }

  naviguerMois(delta: number): void {
    let m = this.mois() + delta;
    let a = this.annee();
    if (m === 0)  { m = 12; a--; }
    if (m === 13) { m = 1;  a++; }
    this.mois.set(m); this.annee.set(a);
    this.paiementId.set(null);
    this.charger();
  }

  ouvrirPaiement(id: string, montantDefaut = 0): void {
    this.paiementId.set(id);
    this.paiementRef.set('');
    this.paiementMontant.set(montantDefaut);
    this.paiementFondAide.set(0);
  }

  validerPaiement(): void {
    const id = this.paiementId();
    if (!id || this.submitting()) return;
    this.submitting.set(true);
    this.svc.enregistrerPaiement(id, {
      referencePaiement: this.paiementRef() || undefined,
      montantTontine:    this.paiementMontant() > 0 ? this.paiementMontant() : undefined,
      montantFondAide:   this.paiementFondAide() > 0 ? this.paiementFondAide() : undefined,
    }).subscribe({
      next: u => {
        this.cotisations.update(l => l.map(c => c.id === id ? u : c));
        this.paiementId.set(null);
        this.submitting.set(false);
      },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  marquerRetard(id: string): void {
    this.svc.marquerEnRetard(id).subscribe({
      next: u => this.cotisations.update(l => l.map(c => c.id === id ? u : c)),
      error: e => this.error.set(e.message ?? 'Erreur'),
    });
  }

  tauxRecouvrement(): number {
    const t = this.cotisations().length;
    return t ? Math.round((this.payees() / t) * 100) : 0;
  }

  moisNom(m: number): string { return MOIS[m] ?? String(m); }
  fcfa(n: number | null): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
  badgeClass(s: string): string { return s === 'PAYEE' ? 'badge-success' : s === 'EN_RETARD' ? 'badge-danger' : 'badge-warning'; }
  statutLabel(s: string): string { return s === 'PAYEE' ? 'Payée' : s === 'EN_RETARD' ? 'En retard' : 'En attente'; }
  formatDate(d: string | null): string { return d ? new Date(d).toLocaleDateString('fr-FR') : '—'; }
}
