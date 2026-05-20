import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { SessionService } from '../../../core/services/session.service';
import { TontineService } from '../../../core/services/tontine.service';
import {
  SessionResponse,
  SessionBilanResponse,
  SessionCotisationsStatutResponse,
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
  cibleMembres: number | null = null;
  sessionOuverte = signal<SessionResponse | null>(null);

  // Bilan
  bilan = signal<SessionBilanResponse | null>(null);
  afficherBilan = signal(false);

  // Statut cotisations
  statutCotisations = signal<SessionCotisationsStatutResponse | null>(null);
  afficherStatut = signal(false);

  // Saisie de séance
  afficherSaisie = signal(false);
  saisieData: Record<string, { montantTontine: number; montantFondAide: number; ref: string }> = {};
  saisieResultat = signal<{ totalEnregistres: number; montantTontineCollecte: number; montantFondAideCollecte: number } | null>(null);

  // Validation bénéfice
  montantValide: { [id: string]: number } = {};

  // Date manuelle session
  nouvelleDateProchaine = '';

  nbGenerees = signal(0);

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
    this.sessionSvc.creerSession({
      tontineId: this.tontineId(),
      dateDebut: this.dateDebut,
      cibleMembres: this.cibleMembres ?? undefined,
    }).subscribe({
      next: session => {
        this.sessions.update(list => [session, ...list]);
        this.sessionOuverte.set(session);
        this.success.set('Session créée avec succès.');
        this.dateDebut = '';
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur création.'); this.saving.set(false); },
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
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur validation.'); this.saving.set(false); },
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
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur date.'); this.saving.set(false); },
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
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur mise à jour date.'); this.saving.set(false); },
    });
  }

  // ── Cotisations session ─────────────────────────────────────────────────────

  genererCotisations(session: SessionResponse): void {
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.genererCotisations(session.id).subscribe({
      next: list => {
        this.nbGenerees.set(list.length);
        this.success.set(`${list.length} cotisation(s) générée(s) pour la session n°${session.numero}.`);
        this.saving.set(false);
        // Recharger le statut automatiquement
        this.chargerStatut(session);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur génération.'); this.saving.set(false); },
    });
  }

  chargerStatut(session: SessionResponse): void {
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.cotisationsStatut(session.id).subscribe({
      next: s => { this.statutCotisations.set(s); this.afficherStatut.set(true); this.saving.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur statut'); this.saving.set(false); },
    });
  }

  badgeStatutCot(s: string): string {
    switch (s) {
      case 'PAYEE':      return 'bg-green-100 text-green-700';
      case 'EN_ATTENTE': return 'bg-amber-100 text-amber-700';
      case 'EN_RETARD':  return 'bg-red-100 text-red-700';
      default:           return 'bg-gray-100 text-gray-500';
    }
  }

  libelleStatutCot(s: string): string {
    switch (s) {
      case 'PAYEE':      return '✓ Payée';
      case 'EN_ATTENTE': return '⏳ En attente';
      case 'EN_RETARD':  return '⚠ En retard';
      default:           return '— Absente';
    }
  }

  // ── Autres ─────────────────────────────────────────────────────────────────

  chargerBilan(session: SessionResponse): void {
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.calculerBilan(session.id).subscribe({
      next: b => { this.bilan.set(b); this.afficherBilan.set(true); this.saving.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur bilan'); this.saving.set(false); },
    });
  }

  cloturerSession(session: SessionResponse, forcer = false): void {
    const msg = forcer
      ? 'Forcer la clôture même si des membres n\'ont pas bénéficié ?'
      : 'Clôturer définitivement cette session ?';
    if (!confirm(msg)) return;
    this.saving.set(true); this.error.set('');
    this.sessionSvc.cloturerSession(session.id, forcer).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set(`Session n°${updated.numero} clôturée.`);
        this.saving.set(false);
      },
      error: e => {
        const msg = e.error?.detail ?? e.error?.message ?? 'Erreur clôture';
        // Si erreur "membres pas encore bénéficiés", proposer le forçage
        if (msg.includes('bénéficié') && !forcer) {
          this.error.set(msg + ' — Cliquez sur "Forcer la clôture" pour ignorer.');
        } else {
          this.error.set(msg);
        }
        this.saving.set(false);
      },
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
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur recalibrage.'); this.saving.set(false); },
    });
  }

  // ── Saisie de séance ────────────────────────────────────────────────────────

  ouvrirSaisieSeance(session: SessionResponse): void {
    this.afficherSaisie.set(true);
    this.saisieResultat.set(null);
    this.error.set('');
    // Initialiser les champs avec les montants par défaut depuis les cotisations
    if (this.statutCotisations()) {
      this.statutCotisations()!.membres.forEach(m => {
        if (m.cotisationId && m.statutCotisation !== 'PAYEE') {
          this.saisieData[m.cotisationId] = { montantTontine: 0, montantFondAide: 0, ref: '' };
        }
      });
    }
  }

  validerSaisieSeance(session: SessionResponse): void {
    const statut = this.statutCotisations();
    if (!statut) return;

    const paiements = statut.membres
      .filter(m => m.cotisationId && m.statutCotisation !== 'PAYEE')
      .map(m => ({
        cotisationId: m.cotisationId!,
        montantTontine:    this.saisieData[m.cotisationId!]?.montantTontine || undefined,
        montantFondAide:   this.saisieData[m.cotisationId!]?.montantFondAide || undefined,
        referencePaiement: this.saisieData[m.cotisationId!]?.ref || undefined,
      }));

    if (paiements.length === 0) {
      this.error.set('Aucun paiement à enregistrer.'); return;
    }

    this.saving.set(true); this.error.set('');
    this.sessionSvc.saisirPaiementsSeance(session.id, paiements).subscribe({
      next: r => {
        this.saisieResultat.set(r);
        this.success.set(`${r.totalEnregistres} paiement(s) enregistré(s).`);
        this.saving.set(false);
        this.afficherSaisie.set(false);
        this.chargerStatut(session);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur saisie'); this.saving.set(false); },
    });
  }

  fcfaSeance(n: number | null | undefined): string {
    if (!n) return '0';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n);
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
