import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { TontineService } from '../../../core/services/tontine.service';
import { SessionService } from '../../../core/services/session.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineResponse, SessionLotResponse } from '../../../core/models/tontine.model';
import { SessionResponse } from '../../../core/models/session.model';
import { MembreResponse } from '../../../core/models/membre.model';

/**
 * Gestion d'une tontine « à lot » : inscription des mises mensuelles, figeage,
 * et affichage des lots/tours (cagnotte, parts par membre, trésorerie).
 */
@Component({
  selector: 'app-tontine-lot',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './tontine-lot.component.html',
})
export class TontineLotComponent implements OnInit {
  private tontineSvc = inject(TontineService);
  private sessionSvc = inject(SessionService);
  private membreSvc  = inject(MembreService);

  tontines = signal<TontineResponse[]>([]);
  membres  = signal<MembreResponse[]>([]);
  session  = signal<SessionResponse | null>(null);
  lot      = signal<SessionLotResponse | null>(null);

  loading = signal(true);
  saving  = signal(false);
  error   = signal('');
  success = signal('');

  tontineId = '';
  membreId = '';
  montantMensuel: number | null = null;

  tontineCourante = computed(() => this.tontines().find(t => t.id === this.tontineId) ?? null);

  ngOnInit(): void {
    this.tontineSvc.getAll().subscribe({
      next: list => {
        const lots = list.filter(t => t.mode === 'A_LOT');
        this.tontines.set(lots);
        if (lots[0]) { this.tontineId = lots[0].id; this.charger(); }
        else { this.loading.set(false); this.error.set('Aucune tontine en mode « à lot ». Activez-le dans la configuration.'); }
      },
      error: () => { this.loading.set(false); this.error.set('Impossible de charger les tontines.'); },
    });
  }

  charger(): void {
    if (!this.tontineId) return;
    this.loading.set(true);
    this.error.set('');
    this.membreSvc.getAll(this.tontineId, 'ACTIF').subscribe({
      next: m => this.membres.set(m.filter(x => x.typeParticipation === 'TONTINE')),
    });
    this.sessionSvc.listerSessions(this.tontineId).subscribe({
      next: list => {
        const s = list.find(x => x.statut === 'EN_COURS') ?? list[0] ?? null;
        this.session.set(s);
        if (s) this.chargerLot(s.id);
        else { this.loading.set(false); this.error.set('Aucune session. Créez une session pour cette tontine d\'abord.'); }
      },
      error: () => { this.loading.set(false); this.error.set('Impossible de charger les sessions.'); },
    });
  }

  chargerLot(sessionId: string): void {
    this.sessionSvc.getLot(sessionId).subscribe({
      next: l => { this.lot.set(l); this.loading.set(false); },
      error: e => { this.error.set(e.error?.detail ?? 'Erreur chargement lot'); this.loading.set(false); },
    });
  }

  adherer(): void {
    const s = this.session();
    if (!s) return;
    if (!this.membreId || !this.montantMensuel || this.montantMensuel <= 0) {
      this.error.set('Sélectionnez un membre et une mise mensuelle valide.'); return;
    }
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.adhererLot(s.id, this.membreId, this.montantMensuel).subscribe({
      next: l => {
        this.lot.set(l);
        this.success.set('Adhésion enregistrée.');
        this.membreId = ''; this.montantMensuel = null;
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur adhésion'); this.saving.set(false); },
    });
  }

  figer(): void {
    const s = this.session();
    if (!s) return;
    if (!confirm('Figer la session ? Les adhésions seront closes et les lots constitués (action automatique en fin de période, mais vous pouvez la déclencher maintenant).')) return;
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.figerLot(s.id).subscribe({
      next: l => { this.lot.set(l); this.success.set('Session figée.'); this.saving.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur figeage'); this.saving.set(false); },
    });
  }

  totalAdhesions(): number {
    return (this.lot()?.adhesions ?? []).reduce((sum, a) => sum + (a.montantMensuel || 0), 0);
  }
}
