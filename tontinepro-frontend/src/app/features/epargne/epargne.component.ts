import { Component, OnInit, signal, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { EpargneService } from '../../core/services/epargne.service';
import { CompteEpargneResponse, MouvementEpargneResponse } from '../../core/models/epargne.model';

@Component({
  selector: 'app-epargne',
  templateUrl: './epargne.component.html',
})
export class EpargneComponent implements OnInit {
  auth = inject(AuthService);
  private svc = inject(EpargneService);

  // Membre
  compte      = signal<CompteEpargneResponse | null>(null);
  mouvements  = signal<MouvementEpargneResponse[]>([]);
  mode        = signal<'depot' | 'retrait'>('depot');
  montant     = signal(0);
  reference   = signal('');

  // Admin
  comptes             = signal<CompteEpargneResponse[]>([]);
  selectedMembreId    = signal<string | null>(null);
  selectedHistorique  = signal<MouvementEpargneResponse[]>([]);
  confirmInterets     = signal(false);
  resultatInterets    = signal<number | null>(null);

  loading     = signal(true);
  submitting  = signal(false);
  error       = signal('');

  ngOnInit(): void {
    if (this.auth.isAdmin()) {
      this.svc.getAllComptes().subscribe({
        next:  data => { this.comptes.set(data); this.loading.set(false); },
        error: e    => { this.error.set(e.message ?? 'Erreur'); this.loading.set(false); },
      });
    } else {
      this.chargerMembreData();
    }
  }

  chargerMembreData(): void {
    this.loading.set(true);
    this.svc.getMonCompte().subscribe({
      next: c => {
        this.compte.set(c);
        this.svc.getHistorique().subscribe({
          next: h => { this.mouvements.set(h); this.loading.set(false); },
          error: () => this.loading.set(false),
        });
      },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.loading.set(false); },
    });
  }

  soumettre(): void {
    if (this.montant() <= 0 || this.submitting()) return;
    this.submitting.set(true);
    this.error.set('');
    const obs = this.mode() === 'depot'
      ? this.svc.depot(this.montant(), this.reference() || undefined)
      : this.svc.retrait(this.montant(), this.reference() || undefined);

    obs.subscribe({
      next: c => {
        this.compte.set(c);
        this.montant.set(0);
        this.reference.set('');
        this.submitting.set(false);
        this.svc.getHistorique().subscribe(h => this.mouvements.set(h));
      },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  voirHistorique(membreId: string): void {
    if (this.selectedMembreId() === membreId) {
      this.selectedMembreId.set(null);
      this.selectedHistorique.set([]);
      return;
    }
    this.selectedMembreId.set(membreId);
    this.svc.getHistoriqueParMembre(membreId).subscribe(h => this.selectedHistorique.set(h));
  }

  distribuerInterets(): void {
    const tontineId = this.comptes()[0]?.tontineId;
    if (!tontineId) return;
    this.submitting.set(true);
    this.svc.distribuerInterets(tontineId).subscribe({
      next: r => {
        this.resultatInterets.set(r.comptesCredites);
        this.confirmInterets.set(false);
        this.submitting.set(false);
        this.svc.getAllComptes().subscribe(c => this.comptes.set(c));
      },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  totalEpargne(): number {
    return this.comptes().reduce((s, c) => s + c.solde, 0);
  }

  fcfa(n: number | null): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  typeIcon(t: string): string {
    return t === 'DEPOT' ? '↑' : t === 'RETRAIT' ? '↓' : '✦';
  }

  typeClass(t: string): string {
    return t === 'DEPOT' ? 'text-green-600' : t === 'RETRAIT' ? 'text-red-600' : 'text-blue-600';
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
