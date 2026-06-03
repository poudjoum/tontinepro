import { Component, OnInit, signal, inject, computed, effect, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe, NgIf } from '@angular/common';
import { Router } from '@angular/router';
import { SessionService } from '../../../core/services/session.service';
import { CotisationService } from '../../../core/services/cotisation.service';
import { TontineService } from '../../../core/services/tontine.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';
import {
  SessionResponse,
  SessionBilanResponse,
  SessionCotisationsStatutResponse,
  OrdreBeneficiaireResponse,
  MembreEligibleRetardResponse,
  InscrireEnRetardResult,
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
  private sessionSvc  = inject(SessionService);
  private cotisationSvc = inject(CotisationService);
  private tontineSvc  = inject(TontineService);
  private ctx         = inject(TontineContextService);
  private router      = inject(Router);

  constructor() {
    // Suit la tontine courante : recharge à chaque changement de sélection.
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => this.onTontineCourante(id));
    });
  }

  private onTontineCourante(id: string): void {
    this.tontineId.set(id);
    this.modeDate.set(this.ctx.tontineCourante()?.typeReglePeriodicite === 'DATE_MANUELLE');
    this.sessionOuverte.set(null);
    this.chargerSessions(id);
  }

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


  // Date manuelle session
  nouvelleDateProchaine = '';

  nbGenerees = signal(0);

  // Echeancier — édition de date par bénéficiaire
  dateEdition: { [id: string]: string } = {};  // ordreBeneficiaireId -> nouvelle date saisie
  editantId = signal<string | null>(null);

  // Correction de cotisation
  cotisationEnEdition = signal<string | null>(null); // cotisationId en cours d'édition
  cotisationEditData: Record<string, { montant: number; montantFondAide: number; montantRepas: number; ref: string; statut: string }> = {};

  // Inscription en retard
  eligiblesRetard = signal<MembreEligibleRetardResponse[]>([]);
  afficherRetard  = signal(false);
  retardResultat  = signal<InscrireEnRetardResult | null>(null);
  membreRetardSelectionne = signal<MembreEligibleRetardResponse | null>(null);
  retardMontantCotis = signal<number | null>(null);
  retardMontantRepas = signal<number | null>(null);
  retardMontantFond  = signal<number>(0);

  sessionEnCours = computed(() =>
    this.sessions().find(s => s.statut === 'EN_COURS') ?? null
  );

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) {
      // Aucune tontine : laisser l'effet réagir quand le contexte sera chargé.
      this.loading.set(false);
    }
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
    this.saving.set(true);
    this.sessionSvc.validerBenefice(session.id, ob.id, {}).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set(`Bénéfice de ${ob.membrePrenom} ${ob.membreNom} validé.`);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur validation.'); this.saving.set(false); },
    });
  }

  annulerBenefice(session: SessionResponse, ob: OrdreBeneficiaireResponse): void {
    if (!confirm(`Annuler la validation du tour de ${ob.membrePrenom} ${ob.membreNom} ?\n`
      + `Le membre repassera en "non bénéficié" : vous pourrez corriger les cotisations du mois puis revalider.`)) return;
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.annulerBenefice(session.id, ob.id).subscribe({
      next: updated => {
        this.sessions.update(list => list.map(s => s.id === updated.id ? updated : s));
        this.sessionOuverte.set(updated);
        this.success.set(`Validation de ${ob.membrePrenom} ${ob.membreNom} annulée.`);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur annulation.'); this.saving.set(false); },
    });
  }

  supprimerSession(session: SessionResponse): void {
    if (!confirm(`⚠️ Supprimer DÉFINITIVEMENT la session n°${session.numero} ?\n\n`
      + `Cela efface l'ordre des bénéficiaires ET toutes les cotisations des mois couverts par la session. `
      + `Cette action est irréversible — à utiliser pour recommencer un rattrapage erroné.`)) return;
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.supprimerSession(session.id).subscribe({
      next: () => {
        this.sessions.update(list => list.filter(s => s.id !== session.id));
        if (this.sessionOuverte()?.id === session.id) this.sessionOuverte.set(null);
        this.afficherStatut.set(false);
        this.afficherSaisie.set(false);
        this.afficherBilan.set(false);
        this.success.set(`Session n°${session.numero} supprimée. Vous pouvez recommencer.`);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur suppression.'); this.saving.set(false); },
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
    // Charger le statut d'abord, puis initialiser la saisie avec les montants par défaut
    this.saving.set(true);
    this.error.set('');
    this.saisieResultat.set(null);
    this.sessionSvc.cotisationsStatut(session.id).subscribe({
      next: s => {
        this.statutCotisations.set(s);
        this.afficherStatut.set(false);
        // Initialiser saisieData avec montants pré-remplis depuis la tontine
        this.saisieData = {};
        const defCot    = s.montantCotisationDefaut ?? 0;
        const defFond   = s.montantFondAideDefaut   ?? 0;
        s.membres.forEach(m => {
          if (m.cotisationId && m.statutCotisation !== 'PAYEE') {
            this.saisieData[m.cotisationId] = {
              montantTontine: defCot,
              montantFondAide: defFond,
              ref: ''
            };
          }
        });
        this.afficherSaisie.set(true);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? 'Erreur chargement statut'); this.saving.set(false); },
    });
  }

  validerSaisieSeance(session: SessionResponse): void {
    const statut = this.statutCotisations();
    if (!statut) return;

    const paiements = statut.membres
      .filter(m => m.cotisationId && m.statutCotisation !== 'PAYEE')
      .map(m => {
        const d = this.saisieData[m.cotisationId!];
        return {
          cotisationId:      m.cotisationId!,
          // Envoyer les montants même à 0 (undefined = utiliser la valeur déjà sur la cotisation)
          montantTontine:    d?.montantTontine != null ? d.montantTontine : undefined,
          montantFondAide:   d?.montantFondAide != null ? d.montantFondAide : undefined,
          referencePaiement: d?.ref || undefined,
        };
      });

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

  voirRapport(sessionId: string, ordreBeneficiaireId: string): void {
    this.router.navigate(['/rapport-tour', sessionId, ordreBeneficiaireId]);
  }

  // ── Correction cotisation ──────────────────────────────────────────────────

  ouvrirEditionCotisation(cotisationId: string): void {
    this.cotisationSvc.getById(cotisationId).subscribe({
      next: c => {
        this.cotisationEditData[cotisationId] = {
          montant: c.montant,
          montantFondAide: c.montantFondAide,
          montantRepas: c.montantRepas,
          ref: c.referencePaiement ?? '',
          statut: c.statut,
        };
        this.cotisationEnEdition.set(cotisationId);
      },
      error: () => this.error.set('Impossible de charger la cotisation.'),
    });
  }

  annulerEditionCotisation(): void {
    this.cotisationEnEdition.set(null);
  }

  sauvegarderCotisation(session: SessionResponse, cotisationId: string): void {
    const d = this.cotisationEditData[cotisationId];
    if (!d) return;
    this.saving.set(true);
    this.error.set('');
    this.cotisationSvc.modifier(cotisationId, {
      montant:          d.montant,
      montantFondAide:  d.montantFondAide,
      montantRepas:     d.montantRepas,
      referencePaiement: d.ref || null,
      statut:           d.statut as any,
    }).subscribe({
      next: () => {
        this.cotisationEnEdition.set(null);
        this.success.set('Cotisation corrigée.');
        this.chargerStatut(session);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur correction'); this.saving.set(false); },
    });
  }

  // ── Inscription en retard ───────────────────────────────────────────────────

  ouvrirInscriptionRetard(session: SessionResponse): void {
    this.afficherRetard.set(true);
    this.retardResultat.set(null);
    this.membreRetardSelectionne.set(null);
    this.retardMontantCotis.set(null);
    this.retardMontantRepas.set(null);
    this.eligiblesRetard.set([]);
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.membresEligiblesRetard(session.id).subscribe({
      next: list => { this.eligiblesRetard.set(list); this.saving.set(false); },
      error: e   => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur chargement membres'); this.saving.set(false); this.afficherRetard.set(false); },
    });
  }

  selectionnerMembreRetard(m: MembreEligibleRetardResponse): void {
    this.membreRetardSelectionne.set(m);
    this.retardMontantCotis.set(m.montantCotisationUnitaire ?? null);
    this.retardMontantRepas.set(m.montantRepasUnitaire ?? null);
    this.retardMontantFond.set(0);
  }

  confirmerInscriptionRetard(session: SessionResponse): void {
    const m = this.membreRetardSelectionne();
    if (!m) return;
    const cotis = this.retardMontantCotis();
    const repas = this.retardMontantRepas();
    const nbTours = m.toursARattraper;
    const totalEstime = ((cotis ?? 0) + (repas ?? 0)) * nbTours;
    if (!confirm(`Inscrire ${m.prenom} ${m.nom} en retard ?\nRattrapage : ${cotis ?? 0} + ${repas ?? 0} FCFA × ${nbTours} tour(s) = ${totalEstime} FCFA`)) return;
    this.saving.set(true);
    const fond = this.retardMontantFond();
    this.sessionSvc.inscrireEnRetard(session.id, m.membreId,
      cotis ?? undefined, repas ?? undefined, fond).subscribe({
      next: r => {
        this.retardResultat.set(r);
        this.sessions.update(list => list.map(s => s.id === r.sessionMiseAJour.id ? r.sessionMiseAJour : s));
        this.sessionOuverte.set(r.sessionMiseAJour);
        this.success.set(`${r.nomMembre} inscrit(e) en position ${r.positionDansSession}. Rattrapage : ${this.fcfaSeance(r.totalRattrapage)} FCFA.`);
        this.saving.set(false);
        this.afficherRetard.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur inscription'); this.saving.set(false); },
    });
  }

  /**
   * Prochain bénéficiaire = premier non-bénéficié trié par dateBenefice ASC,
   * puis par ordre si dates identiques ou absentes.
   */
  prochainBeneficiaire(session: SessionResponse): OrdreBeneficiaireResponse | null {
    return this.beneficiairesTriesParDate(session).find(b => !b.beneficie) ?? null;
  }

  /** Liste triée par dateBenefice ASC (nulls en dernier), puis par ordre.
   *  Les membres RETIRE qui n'ont pas encore bénéficié sont exclus de l'affichage. */
  beneficiairesTriesParDate(session: SessionResponse): OrdreBeneficiaireResponse[] {
    return [...session.beneficiaires]
      .filter(ob => ob.beneficie || ob.membreStatut !== 'RETIRE')
      .sort((a, b) => {
        if (!a.dateBenefice && !b.dateBenefice) return a.ordre - b.ordre;
        if (!a.dateBenefice) return 1;
        if (!b.dateBenefice) return -1;
        const diff = new Date(a.dateBenefice).getTime() - new Date(b.dateBenefice).getTime();
        return diff !== 0 ? diff : a.ordre - b.ordre;
      });
  }

  datePassee(dateStr: string | null): boolean {
    if (!dateStr) return false;
    return new Date(dateStr).setHours(23, 59, 59, 999) < Date.now();
  }

  joursRestants(dateStr: string | null): string {
    if (!dateStr) return '—';
    const diff = Math.ceil((new Date(dateStr).getTime() - Date.now()) / 86400000);
    if (diff < 0) return 'Passé';
    if (diff === 0) return "Aujourd'hui";
    return `Dans ${diff} jour${diff > 1 ? 's' : ''}`;
  }
}
