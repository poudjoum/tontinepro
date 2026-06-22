import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TontineService } from '../../../core/services/tontine.service';
import { SessionService } from '../../../core/services/session.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineResponse } from '../../../core/models/tontine.model';
import {
  SessionResponse,
  OrdreBeneficiaireResponse,
  SessionCotisationsStatutResponse,
} from '../../../core/models/session.model';
import { MembreResponse } from '../../../core/models/membre.model';

type Etape = 'creation' | 'rattrapage' | 'termine';

interface SaisieLigne {
  montantTontine: number;
  montantFondAide: number;
  montantRepas: number;
  ref: string;
}

/**
 * Assistant de reprise d'une tontine déjà démarrée hors application.
 * Étape 1 : créer la session backdatée (date de début passée) + ordre des membres.
 * Étape 2 : pour chaque tour antérieur (mois par mois), saisir les cotisations puis
 *           valider le bénéficiaire (montant reçu calculé automatiquement sur ce mois).
 * Étape 3 : la session est à jour au mois courant.
 */
@Component({
  selector: 'app-reprise-session',
  imports: [FormsModule],
  templateUrl: './reprise-session.component.html',
})
export class RepriseSessionComponent implements OnInit {
  private tontineSvc = inject(TontineService);
  private sessionSvc = inject(SessionService);
  private membreSvc  = inject(MembreService);
  private router     = inject(Router);

  tontines = signal<TontineResponse[]>([]);
  membres  = signal<MembreResponse[]>([]);   // membres TONTINE actifs, dans l'ordre choisi

  loading = signal(false);
  saving  = signal(false);
  error   = signal('');
  success = signal('');

  etape   = signal<Etape>('creation');

  tontineId = '';
  dateDebut = '';

  session      = signal<SessionResponse | null>(null);
  toursPasses  = signal<OrdreBeneficiaireResponse[]>([]);
  tourIdx      = signal(0);

  /** Session EN_COURS déjà existante pour la tontine (reprise à continuer). */
  sessionExistante = signal<SessionResponse | null>(null);

  // Saisie du tour courant
  statut    = signal<SessionCotisationsStatutResponse | null>(null);
  saisie: Record<string, SaisieLigne> = {};
  cotisationsEnregistrees = signal(false);

  ngOnInit(): void {
    this.tontineSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        if (list[0]) { this.tontineId = list[0].id; this.chargerMembres(); }
      },
      error: () => this.error.set('Impossible de charger les tontines.'),
    });
  }

  chargerMembres(): void {
    if (!this.tontineId) return;
    this.error.set('');
    this.membreSvc.getAll(this.tontineId, 'ACTIF').subscribe({
      next: list => {
        // Seuls les membres TONTINE participent au calendrier
        this.membres.set(list.filter(m => m.typeParticipation === 'TONTINE'));
      },
      error: () => this.error.set('Impossible de charger les membres.'),
    });
    // Détecter une reprise déjà en cours (session EN_COURS) pour la continuer
    this.sessionSvc.listerSessions(this.tontineId).subscribe({
      next: list => this.sessionExistante.set(list.find(s => s.statut === 'EN_COURS') ?? null),
      error: () => this.sessionExistante.set(null),
    });
  }

  /** Nombre de tours passés non encore validés sur la session existante. */
  toursARattraper(s: SessionResponse): number {
    const now = new Date();
    const repere = now.getFullYear() * 12 + now.getMonth();
    return s.beneficiaires.filter(ob => ob.dateBenefice && !ob.beneficie
      && (new Date(ob.dateBenefice).getFullYear() * 12 + new Date(ob.dateBenefice).getMonth()) < repere).length;
  }

  /** Reprendre une session existante : continuer sur les tours non encore validés. */
  continuer(): void {
    const s = this.sessionExistante();
    if (!s) return;
    this.session.set(s);
    this.success.set('');
    this.error.set('');
    this.demarrerRattrapage(s);
  }

  // ── Étape 1 : ordre des membres ───────────────────────────────────────────

  monter(i: number): void {
    if (i <= 0) return;
    const arr = [...this.membres()];
    [arr[i - 1], arr[i]] = [arr[i], arr[i - 1]];
    this.membres.set(arr);
  }

  descendre(i: number): void {
    const arr = [...this.membres()];
    if (i >= arr.length - 1) return;
    [arr[i + 1], arr[i]] = [arr[i], arr[i + 1]];
    this.membres.set(arr);
  }

  creerSession(): void {
    if (!this.dateDebut) { this.error.set('Veuillez saisir la date de début (mois de démarrage réel).'); return; }
    if (this.membres().length === 0) { this.error.set('Aucun membre actif à inscrire.'); return; }
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.creerSession({
      tontineId: this.tontineId,
      dateDebut: this.dateDebut,
      ordreMembreIds: this.membres().map(m => m.id),
    }).subscribe({
      next: session => {
        this.session.set(session);
        this.demarrerRattrapage(session);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur création de session.'); this.saving.set(false); },
    });
  }

  // ── Étape 2 : rattrapage mois par mois ─────────────────────────────────────

  /** Tours dont la date de passage est antérieure au mois courant et non encore validés. */
  private demarrerRattrapage(session: SessionResponse): void {
    const now = new Date();
    const repereCourant = now.getFullYear() * 12 + now.getMonth(); // année*12 + mois (0-based)

    const passes = [...session.beneficiaires]
      .filter(ob => ob.dateBenefice && !ob.beneficie)
      .filter(ob => {
        const d = new Date(ob.dateBenefice!);
        return (d.getFullYear() * 12 + d.getMonth()) < repereCourant;
      })
      .sort((a, b) =>
        new Date(a.dateBenefice!).getTime() - new Date(b.dateBenefice!).getTime());

    this.toursPasses.set(passes);
    this.tourIdx.set(0);

    if (passes.length === 0) {
      this.etape.set('termine');
      this.success.set('Session créée. Aucun tour antérieur à rattraper — vous pouvez opérer normalement pour le mois courant.');
    } else {
      this.etape.set('rattrapage');
      this.chargerTour();
    }
  }

  tourCourant(): OrdreBeneficiaireResponse | null {
    return this.toursPasses()[this.tourIdx()] ?? null;
  }

  moisCourant(): number {
    const ob = this.tourCourant();
    return ob?.dateBenefice ? new Date(ob.dateBenefice).getMonth() + 1 : 0;
  }

  anneeCourante(): number {
    const ob = this.tourCourant();
    return ob?.dateBenefice ? new Date(ob.dateBenefice).getFullYear() : 0;
  }

  /** Génère puis charge le statut des cotisations du mois du tour courant. */
  chargerTour(): void {
    const sess = this.session();
    const ob = this.tourCourant();
    if (!sess || !ob) return;
    const mois = this.moisCourant();
    const annee = this.anneeCourante();

    this.saving.set(true);
    this.error.set('');
    this.cotisationsEnregistrees.set(false);

    this.sessionSvc.genererCotisations(sess.id, mois, annee).subscribe({
      next: () => {
        this.sessionSvc.cotisationsStatut(sess.id, mois, annee).subscribe({
          next: s => {
            this.statut.set(s);
            this.saisie = {};
            const defCot  = s.montantCotisationDefaut ?? 0;
            const defFond = s.montantFondAideDefaut   ?? 0;
            s.membres.forEach(m => {
              if (m.cotisationId) {
                // Pré-remplissage : montants déjà saisis si la cotisation existe,
                // sinon valeurs par défaut de la tontine pour une nouvelle saisie.
                const dejaSaisie = m.statutCotisation === 'PAYEE';
                this.saisie[m.cotisationId] = {
                  montantTontine:  m.montantTontine  ?? (dejaSaisie ? 0 : defCot),
                  montantFondAide: m.montantFondAide ?? (dejaSaisie ? 0 : defFond),
                  montantRepas:    m.montantRepas    ?? 0,
                  ref:             m.referencePaiement ?? '',
                };
              }
            });
            this.saving.set(false);
          },
          error: e => { this.error.set(e.error?.detail ?? 'Erreur chargement du statut.'); this.saving.set(false); },
        });
      },
      error: e => { this.error.set(e.error?.detail ?? 'Erreur génération des cotisations.'); this.saving.set(false); },
    });
  }

  /** Enregistre les cotisations saisies pour le mois du tour courant. */
  enregistrerCotisations(): void {
    const sess = this.session();
    const s = this.statut();
    if (!sess || !s) return;

    // Reprise : on (re)saisit toutes les lignes, y compris les cotisations déjà
    // PAYEE (correction des tours passés).
    const paiements = s.membres
      .filter(m => m.cotisationId)
      .map(m => {
        const d = this.saisie[m.cotisationId!];
        return {
          cotisationId:      m.cotisationId!,
          montantTontine:    d?.montantTontine ?? 0,
          montantFondAide:   d?.montantFondAide ?? 0,
          montantRepas:      d?.montantRepas ?? 0,
          referencePaiement: d?.ref || undefined,
        };
      });

    if (paiements.length === 0) {
      this.cotisationsEnregistrees.set(true);
      this.success.set('Aucun membre à saisir pour ce mois.');
      return;
    }

    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.saisirPaiementsSeance(sess.id, paiements, true).subscribe({
      next: r => {
        this.success.set(`${r.totalEnregistres} cotisation(s) enregistrée(s) pour ${this.moisCourant()}/${this.anneeCourante()}.`);
        this.cotisationsEnregistrees.set(true);
        this.saving.set(false);
        // Recharger le statut pour refléter les PAYEE
        this.sessionSvc.cotisationsStatut(sess.id, this.moisCourant(), this.anneeCourante()).subscribe({
          next: st => this.statut.set(st),
        });
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur enregistrement.'); this.saving.set(false); },
    });
  }

  /** Valide le bénéficiaire du tour courant (montant auto-calculé) puis passe au suivant. */
  validerEtSuivant(): void {
    const sess = this.session();
    const ob = this.tourCourant();
    if (!sess || !ob) return;

    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.validerBenefice(sess.id, ob.id, {}).subscribe({
      next: updated => {
        this.session.set(updated);
        this.success.set(`Tour de ${ob.membrePrenom} ${ob.membreNom} validé.`);
        this.saving.set(false);

        const suivant = this.tourIdx() + 1;
        if (suivant >= this.toursPasses().length) {
          this.etape.set('termine');
        } else {
          this.tourIdx.set(suivant);
          this.chargerTour();
        }
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur validation du bénéfice.'); this.saving.set(false); },
    });
  }

  allerAuxSessions(): void {
    this.router.navigate(['/admin/sessions']);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  moisLabel(mois: number, annee: number): string {
    const noms = ['', 'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
      'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'];
    return mois >= 1 && mois <= 12 ? `${noms[mois]} ${annee}` : '—';
  }

  badgeStatutCot(s: string): string {
    switch (s) {
      case 'PAYEE':      return 'bg-green-100 text-green-700';
      case 'EN_ATTENTE': return 'bg-amber-100 text-amber-700';
      case 'EN_RETARD':  return 'bg-red-100 text-red-700';
      default:           return 'bg-gray-100 text-gray-500';
    }
  }
}
