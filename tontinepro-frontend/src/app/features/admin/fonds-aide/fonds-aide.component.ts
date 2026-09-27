import { Component, OnInit, signal, computed, inject, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { SessionService } from '../../../core/services/session.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';
import { FondsAideService } from '../../../core/services/fonds-aide.service';
import { FondsAideMensuelResponse } from '../../../core/models/session.model';

@Component({
  selector: 'app-fonds-aide',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './fonds-aide.component.html',
})
export class FondsAideComponent implements OnInit {
  private sessionSvc = inject(SessionService);
  private ctx        = inject(TontineContextService);
  private fondsSvc   = inject(FondsAideService);

  data     = signal<FondsAideMensuelResponse | null>(null);
  loading  = signal(true);
  error    = signal('');
  today    = new Date().toISOString();

  /** Aucune session en cours pour la tontine courante. */
  aucuneSession = signal(false);

  nbMois = computed(() => this.data()?.mois.length ?? 0);
  totalSolde = computed(() => (this.data()?.membres ?? []).reduce((s, l) => s + l.solde, 0));

  private MOIS_COURT = ['', 'Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin',
    'Juil', 'Août', 'Sept', 'Oct', 'Nov', 'Déc'];

  constructor() {
    // Suit la tontine courante : recharge à chaque changement de sélection.
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => this.charger(id));
    });
  }

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) this.loading.set(false);
  }

  /** Membre dont le rattrapage attend confirmation, puis en cours d'encaissement. */
  confirmRattrapage = signal<string | null>(null);
  encaissement      = signal<string | null>(null);
  success           = signal('');

  demanderRattrapage(membreId: string): void {
    this.success.set('');
    this.error.set('');
    this.confirmRattrapage.set(membreId);
  }

  annulerRattrapage(): void { this.confirmRattrapage.set(null); }

  encaisserRattrapage(membreId: string, nom: string): void {
    const d = this.data();
    if (!d) return;
    this.encaissement.set(membreId);
    this.fondsSvc.encaisserRattrapage(this.tontineId, membreId, d.aides.map(a => a.aideId)).subscribe({
      next: r => {
        this.encaissement.set(null);
        this.confirmRattrapage.set(null);
        this.success.set(`Rattrapage de ${nom} encaissé : ${this.fcfa(r.montantEncaisse)}.`);
        this.charger(this.tontineId, false);
      },
      error: e => {
        this.encaissement.set(null);
        this.confirmRattrapage.set(null);
        // Un 403 signifie « ce n'est pas vous qui encaissez », pas une panne.
        this.error.set(e.status === 403
          ? (e.error?.detail ?? 'L\'encaissement du rattrapage revient au Trésorier de la tontine.')
          : (e.error?.detail ?? e.error?.message ?? 'Encaissement impossible.'));
      },
    });
  }

  private tontineId = '';

  private charger(tontineId: string, reinitialiser = true): void {
    this.tontineId = tontineId;
    // Après un encaissement, on garde le tableau affiché pendant le rechargement
    // (pas de spinner ni de retour en haut de page).
    if (reinitialiser) {
      this.loading.set(true);
      this.error.set('');
      this.success.set('');
      this.data.set(null);
    }
    this.aucuneSession.set(false);

    this.sessionSvc.listerSessions(tontineId).subscribe({
      next: list => {
        const enCours = list.find(s => s.statut === 'EN_COURS');
        if (!enCours) { this.aucuneSession.set(true); this.loading.set(false); return; }
        this.sessionSvc.getFondsAideMensuel(enCours.id).subscribe({
          next: d  => { this.data.set(d); this.loading.set(false); },
          error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur de chargement'); this.loading.set(false); },
        });
      },
      error: () => { this.error.set('Impossible de charger les sessions.'); this.loading.set(false); },
    });
  }

  moisLabel(mois: number, annee: number): string {
    return `${this.MOIS_COURT[mois] ?? ''} ${String(annee).slice(-2)}`;
  }

  dateCourte(iso: string): string {
    const [a, m, j] = iso.split('-');
    return `${j}/${m}/${a.slice(-2)}`;
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  telechargement = signal(false);

  /** Génère et télécharge le PDF paysage produit côté serveur (iText). */
  telechargerPdf(): void {
    const d = this.data();
    if (!d) return;
    this.telechargement.set(true);
    this.sessionSvc.telechargerFondsAideMensuelPdf(d.sessionId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `fonds-aide-session-${d.sessionNumero}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.telechargement.set(false);
      },
      error: () => { this.error.set('Erreur téléchargement PDF'); this.telechargement.set(false); },
    });
  }
}
