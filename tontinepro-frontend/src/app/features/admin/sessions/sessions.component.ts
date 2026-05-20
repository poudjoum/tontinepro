import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { SessionService } from '../../../core/services/session.service';
import { TontineService } from '../../../core/services/tontine.service';
import {
  SessionResponse,
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
  modeDate     = signal(false); // true = DATE_MANUELLE

  // Formulaire création
  dateDebut = '';

  // Session courante ouverte
  sessionOuverte = signal<SessionResponse | null>(null);

  // Validation bénéfice
  montantValide: { [ordreBeneficiaireId: string]: number } = {};

  // Mise à jour date manuelle
  nouvelleDateProchaine = '';

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
      error: () => {
        this.loading.set(false);
        this.error.set('Impossible de charger la tontine.');
      },
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
      error: () => {
        this.error.set('Impossible de charger les sessions.');
        this.loading.set(false);
      },
    });
  }

  creerSession(): void {
    if (!this.dateDebut) {
      this.error.set('Veuillez saisir une date de début.');
      return;
    }
    const req: CreerSessionRequest = {
      tontineId: this.tontineId(),
      dateDebut: this.dateDebut,
    };
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.sessionSvc.creerSession(req).subscribe({
      next: session => {
        this.sessions.update(list => [session, ...list]);
        this.sessionOuverte.set(session);
        this.success.set('Session créée avec succès.');
        this.dateDebut = '';
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors de la création de la session.');
        this.saving.set(false);
      },
    });
  }

  validerBenefice(session: SessionResponse, ob: OrdreBeneficiaireResponse): void {
    const montant = this.montantValide[ob.id];
    if (montant == null || montant < 0) {
      this.error.set('Veuillez saisir un montant valide.');
      return;
    }
    const req: ValiderBeneficeRequest = { montantRecu: montant };
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.validerBenefice(session.id, ob.id, req).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set(`Bénéfice de ${ob.membreNom} ${ob.membrePrenom} validé.`);
        delete this.montantValide[ob.id];
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors de la validation.');
        this.saving.set(false);
      },
    });
  }

  mettreAJourDate(session: SessionResponse): void {
    if (!this.nouvelleDateProchaine) {
      this.error.set('Veuillez saisir une date.');
      return;
    }
    const req: MiseAJourDateRequest = { dateProchaineTontine: this.nouvelleDateProchaine };
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.mettreAJourProchainDate(session.id, req).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set('Date de prochaine réunion mise à jour.');
        this.nouvelleDateProchaine = '';
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors de la mise à jour de la date.');
        this.saving.set(false);
      },
    });
  }

  ouvrirSession(s: SessionResponse): void {
    this.sessionOuverte.set(s);
  }

  recalibrer(session: SessionResponse): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.sessionSvc.recalibrerMembres(session.id).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set(
          `Session recalibrée — ${updated.nombreMembres} membre(s) dans la rotation.`
        );
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors du recalibrage.');
        this.saving.set(false);
      },
    });
  }

  prochainBeneficiaire(session: SessionResponse): OrdreBeneficiaireResponse | null {
    return session.beneficiaires.find(b => !b.beneficie) ?? null;
  }
}
