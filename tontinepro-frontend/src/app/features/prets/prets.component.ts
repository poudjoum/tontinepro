import { Component, OnInit, signal, inject, computed, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { PretService } from '../../core/services/pret.service';
import { MembreService } from '../../core/services/membre.service';
import { TontineContextService } from '../../core/services/tontine-context.service';
import { DocumentService } from '../../core/services/document.service';
import { PretResponse, EcheancePretResponse, SimulationPretResponse } from '../../core/models/pret.model';
import { DocumentResponse } from '../../core/models/document.model';

type VueMembre = 'liste' | 'demande' | 'simulation';
type FiltreAdmin = '' | 'DEMANDE' | 'EN_COURS' | 'SOLDE' | 'REJETE';

const STATUT_LABEL: Record<string, string> = {
  DEMANDE: 'En demande', VALIDE: 'Validé', EN_COURS: 'En cours',
  REJETE: 'Rejeté', SOLDE: 'Soldé',
};
const STATUT_BADGE: Record<string, string> = {
  DEMANDE: 'badge-warning', VALIDE: 'badge-info', EN_COURS: 'badge-info',
  REJETE: 'badge-danger', SOLDE: 'badge-success',
};

@Component({
  selector: 'app-prets',
  imports: [RouterLink],
  templateUrl: './prets.component.html',
})
export class PretsComponent implements OnInit {
  auth = inject(AuthService);
  private svc = inject(PretService);
  private membreSvc = inject(MembreService);
  private docSvc = inject(DocumentService);
  private ctx = inject(TontineContextService);

  constructor() {
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) {
        this.tontineId.set(id);
        untracked(() => {
          this.charger();
          if (!this.auth.isAdmin()) this.chargerMesDocs();
        });
      }
    });
  }

  prets       = signal<PretResponse[]>([]);
  echeances   = signal<EcheancePretResponse[]>([]);
  simulation  = signal<SimulationPretResponse | null>(null);
  loading     = signal(true);
  submitting  = signal(false);
  error       = signal('');
  mesDocs     = signal<DocumentResponse[]>([]);

  hasPlanLocalisation   = computed(() => this.mesDocs().some(d => d.typeDocument === 'PLAN_LOCALISATION'));
  hasReconnDette        = computed(() => this.mesDocs().some(d => d.typeDocument === 'RECONNAISSANCE_DETTE'));
  docsCompletsPourPret  = computed(() => this.hasPlanLocalisation() && this.hasReconnDette());

  // Membre
  vue         = signal<VueMembre>('liste');
  tontineId   = signal('');
  simMontant  = signal(0);
  simDuree    = signal(12);
  demMontant  = signal(0);
  demDuree    = signal(12);

  // Admin
  filtre      = signal<FiltreAdmin>('');
  rejetId     = signal<string | null>(null);
  rejetMotif  = signal('');
  echeanceId  = signal<string | null>(null);

  ngOnInit(): void {
    this.ctx.init();
  }

  chargerMesDocs(): void {
    this.docSvc.mesDocuments(this.tontineId() || undefined)
      .subscribe({ next: d => this.mesDocs.set(d), error: () => {} });
  }

  charger(): void {
    this.loading.set(true);
    const obs = this.auth.isAdmin()
      ? this.svc.getAll(this.filtre() || undefined)
      : this.svc.getMesPrets(this.tontineId() || undefined);
    obs.subscribe({
      next:  data => { this.prets.set(data); this.loading.set(false); },
      error: e    => { this.error.set(e.message ?? 'Erreur'); this.loading.set(false); },
    });
  }

  filtrer(f: FiltreAdmin): void { this.filtre.set(f); this.charger(); }

  voirEcheances(pretId: string): void {
    if (this.echeanceId() === pretId) { this.echeanceId.set(null); return; }
    this.echeanceId.set(pretId);
    this.svc.getEcheances(pretId).subscribe(e => this.echeances.set(e));
  }

  rembourser(pretId: string): void {
    this.submitting.set(true);
    this.svc.rembourser(pretId).subscribe({
      next: () => { this.submitting.set(false); this.charger(); if (this.echeanceId() === pretId) this.voirEcheances(pretId); },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  soumettreDemande(): void {
    if (this.demMontant() <= 0 || this.demDuree() <= 0 || this.submitting()) return;
    this.submitting.set(true);
    this.svc.demande(this.demMontant(), this.demDuree()).subscribe({
      next: () => { this.submitting.set(false); this.vue.set('liste'); this.charger(); },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  simuler(): void {
    if (!this.tontineId() || this.simMontant() <= 0) return;
    this.submitting.set(true);
    this.svc.simuler(this.simMontant(), this.simDuree(), this.tontineId()).subscribe({
      next: s => { this.simulation.set(s); this.submitting.set(false); },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  valider(pretId: string): void {
    this.submitting.set(true);
    this.svc.valider(pretId).subscribe({
      next: u => { this.prets.update(l => l.map(p => p.id === pretId ? u : p)); this.submitting.set(false); },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  ouvrirRejet(pretId: string): void { this.rejetId.set(pretId); this.rejetMotif.set(''); }

  confirmerRejet(): void {
    const id = this.rejetId();
    if (!id || !this.rejetMotif().trim() || this.submitting()) return;
    this.submitting.set(true);
    this.svc.rejeter(id, this.rejetMotif()).subscribe({
      next: u => { this.prets.update(l => l.map(p => p.id === id ? u : p)); this.rejetId.set(null); this.submitting.set(false); },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  progressionPret(p: PretResponse): number {
    const echs = this.echeanceId() === p.id ? this.echeances() : [];
    const payees = echs.filter(e => e.statut === 'PAYEE').length;
    return p.dureeMois ? Math.round((payees / p.dureeMois) * 100) : 0;
  }

  pretEnCours(): PretResponse | undefined {
    return this.prets().find(p => p.statut === 'EN_COURS');
  }

  fcfa(n: number | null): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
  statutLabel(s: string): string { return STATUT_LABEL[s] ?? s; }
  statutBadge(s: string): string { return STATUT_BADGE[s] ?? 'badge-gray'; }
  formatDate(d: string | null): string { return d ? new Date(d).toLocaleDateString('fr-FR') : '—'; }
}
