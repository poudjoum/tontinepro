import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { SessionService } from '../../../core/services/session.service';
import { TontineService } from '../../../core/services/tontine.service';
import {
  SessionResponse,
  SessionBilanResponse,
  OrdreBeneficiaireResponse,
  CreerSessionRequest,
  MiseAJourDateRequest,
  ValiderBeneficeRequest,
} from '../../../core/models/session.model';

@Component({
  selector: 'app-sessions',
  imports: [FormsModule, DatePipe, DecimalPipe],
  templateUrl: './sessions.component.html',
})
export class SessionsComponent implements OnInit {
  private sessionSvc = inject(SessionService);
  private tontineSvc = inject(TontineService);

  sessions     = signal<SessionResponse[]>([]);
  loading      = signal(true);
  saving       = signal(false);
  error        = signal('');
  success      = signal('');
  tontineId    = signal('');
  modeDate     = signal(false);

  dateDebut = '';
  sessionOuverte = signal<SessionResponse | null>(null);

  // Bilan
  bilan = signal<SessionBilanResponse | null>(null);
  afficherBilan = signal(false);

  // Validation bénéfice
  montantValide: { [id: string]: number } = {};

  // Date manuelle session
  nouvelleDateProchaine = '';

  // Echeancier — édition de date par bénéficiaire
  dateEdition: { [id: string]: string } = {};  // ordreBeneficiaireId -> nouvelle date saisie
  editantId = signal<string | null>(null);

  sessionEnCours = computed(() =>
    this.sessions().find(s => s.statut === 'EN_COURS') ?? null
  );

  ngOnInit(): void {
    this.tontineSvc.getAll().subscribe({
      next: list => {
        const t = list[0];
        if (t) {
          this.tontineId.set(t.id);
          this.modeDate.set(t.typeReglePeriodicite === 'DATE_MANUELLE');
          this.chargerSessions(t.id);
        } else {
          this.loading.set(false);
          this.error.set('Aucune tontine configurée.');
        }
      },
      error: () => { this.loading.set(false); this.error.set('Impossible de charger la tontine.'); },
    });
  }

  chargerSessions(tontineId: string): void {
    this.sessionSvc.listerSessions(tontineId).subscribe({
      next: list => {
        this.sessions.set(list);
        const enCours = list.find(s => s.statut === 'EN_COURS');
        if (enCours) this.sessionOuverte.set(enCours);
        this.loading.set(false);
      },
      error: () => { this.error.set('Impossible de charger les sessions.'); this.loading.set(false); },
    });
  }

  creerSession(): void {
    if (!this.dateDebut) { this.error.set('Veuillez saisir une date de début.'); return; }
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.creerSession({ tontineId: this.tontineId(), dateDebut: this.dateDebut }).subscribe({
      next: session => {
        this.sessions.update(list => [session, ...list]);
        this.sessionOuverte.set(session);
        this.success.set('Session créée avec succès.');
        this.dateDebut = '';
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.message ?? 'Erreur création.'); this.saving.set(false); },
    });
  }

  validerBenefice(session: SessionResponse, ob: OrdreBeneficiaireResponse): void {
    const montant = this.montantValide[ob.id];
    if (montant == null || montant < 0) { this.error.set('Veuillez saisir un montant valide.'); return; }
    this.saving.set(true);
    this.sessionSvc.validerBenefice(session.id, ob.id, { montantRecu: montant }).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set(`Bénéfice de ${ob.membrePrenom} ${ob.membreNom} validé.`);
        delete this.montantValide[ob.id];
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.message ?? 'Erreur validation.'); this.saving.set(false); },
    });
  }

  mettreAJourDate(session: SessionResponse): void {
    if (!this.nouvelleDateProchaine) { this.error.set('Veuillez saisir une date.'); return; }
    this.saving.set(true);
    this.sessionSvc.mettreAJourProchainDate(session.id, { dateProchaineTontine: this.nouvelleDateProchaine }).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set('Date mise à jour.');
        this.nouvelleDateProchaine = '';
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.message ?? 'Erreur date.'); this.saving.set(false); },
    });
  }

  // ── Echeancier ─────────────────────────────────────────────────────────────

  ouvrirEditionDate(obId: string, dateActuelle: string | null): void {
    this.editantId.set(obId);
    this.dateEdition[obId] = dateActuelle ?? '';
  }

  annulerEdition(): void {
    this.editantId.set(null);
  }

  sauvegarderDateBenefice(session: SessionResponse, obId: string): void {
    const date = this.dateEdition[obId];
    if (!date) return;
    this.saving.set(true);
    this.sessionSvc.mettreAJourDateBenefice(session.id, obId, date).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.editantId.set(null);
        this.success.set('Date de passage mise à jour.');
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.message ?? 'Erreur mise à jour date.'); this.saving.set(false); },
    });
  }

  // ── Autres ─────────────────────────────────────────────────────────────────

  chargerBilan(session: SessionResponse): void {
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.calculerBilan(session.id).subscribe({
      next: b => { this.bilan.set(b); this.afficherBilan.set(true); this.saving.set(false); },
      error: e => { this.error.set(e.error?.message ?? 'Erreur bilan'); this.saving.set(false); },
    });
  }

  recalibrer(session: SessionResponse): void {
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.recalibrerMembres(session.id).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set(`Session recalibrée — ${updated.nombreMembres} membre(s).`);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.message ?? 'Erreur recalibrage.'); this.saving.set(false); },
    });
  }

  ouvrirSession(s: SessionResponse): void { this.sessionOuverte.set(s); }

  prochainBeneficiaire(session: SessionResponse): OrdreBeneficiaireResponse | null {
    return session.beneficiaires.find(b => !b.beneficie) ?? null;
  }

  joursRestants(dateStr: string | null): string {
    if (!dateStr) return '—';
    const diff = Math.ceil((new Date(dateStr).getTime() - Date.now()) / 86400000);
    if (diff < 0) return 'Passé';
    if (diff === 0) return "Aujourd'hui";
    return `Dans ${diff} jour${diff > 1 ? 's' : ''}`;
  }
}
