import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, DatePipe, NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { TontineService } from '../../../core/services/tontine.service';
import { SessionService } from '../../../core/services/session.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineResponse, SessionLotResponse, CompositionLotsResponse, PartLotMembre, AbandonLot, MembreSessionLot, CotisantRemboursementLot } from '../../../core/models/tontine.model';
import { SessionResponse } from '../../../core/models/session.model';
import { MembreResponse } from '../../../core/models/membre.model';

/** Part d'un membre dans un lot en cours de composition. */
interface PartEdit { cle: number; membreId: string; nom: string; prenom: string; montant: number; }

/** Groupe destinataire d'un déplacement, en plus des index de lots. */
const NOUVEAU_LOT = -1;
const NON_PLACES = -2;

/**
 * Gestion d'une tontine « à lot » : inscription des mises mensuelles, composition
 * des lots (proposée en combinaisons exactes, ajustable par le bureau), figeage,
 * et affichage des lots/tours (cagnotte, parts par membre, trésorerie).
 */
@Component({
  selector: 'app-tontine-lot',
  imports: [FormsModule, DecimalPipe, DatePipe, NgTemplateOutlet, RouterLink],
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

  // ── Composition des lots (avant figeage) ──────────────────────────────────
  readonly NOUVEAU_LOT = NOUVEAU_LOT;
  readonly NON_PLACES = NON_PLACES;
  compoLots   = signal<PartEdit[][] | null>(null);
  compoRestes = signal<PartEdit[]>([]);
  composing   = signal(false);
  /** Part sélectionnée pour un déplacement : groupe (index de lot ou NON_PLACES) + clé. */
  selection   = signal<{ groupe: number; cle: number } | null>(null);
  destination = NON_PLACES;
  montantDeplace: number | null = null;
  private prochaineCle = 1;

  montantLot  = computed(() => this.lot()?.montantLot ?? 0);
  nombreTours = computed(() => this.compoLots()?.length ?? 0);
  lotsInvalides = computed(() =>
    (this.compoLots() ?? []).filter(l => !this.egal(this.totalParts(l), this.montantLot())).length);
  peutFiger = computed(() =>
    this.nombreTours() > 0 && this.compoRestes().length === 0 && this.lotsInvalides() === 0);
  raisonBlocage = computed(() => {
    if (this.nombreTours() === 0) return 'Aucun lot complet.';
    const n = this.compoRestes().length;
    if (n > 0) return `${n} part(s) non placée(s) : placez-les dans un lot ou ajustez les mises.`;
    const k = this.lotsInvalides();
    if (k > 0) return `${k} lot(s) ne font pas exactement ${this.montantLot().toLocaleString('fr-FR')} FCFA.`;
    return '';
  });

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
        if (this.compoLots()) {
          this.abandonnerComposition();
          this.success.set('Adhésion enregistrée. Relancez la composition des lots.');
        }
        this.membreId = ''; this.montantMensuel = null;
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur adhésion'); this.saving.set(false); },
    });
  }

  /** Demande au serveur une composition en combinaisons exactes (rien n'est enregistré). */
  proposerComposition(): void {
    const s = this.session();
    if (!s) return;
    this.composing.set(true);
    this.error.set('');
    this.success.set('');
    this.sessionSvc.getCompositionLot(s.id).subscribe({
      next: c => { this.appliquerProposition(c); this.composing.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur de composition'); this.composing.set(false); },
    });
  }

  private appliquerProposition(c: CompositionLotsResponse): void {
    const versEdit = (p: { membreId: string; nom: string; prenom: string; montant: number }): PartEdit =>
      ({ cle: this.prochaineCle++, membreId: p.membreId, nom: p.nom, prenom: p.prenom, montant: p.montant });
    this.compoLots.set(c.lots.map(l => l.parts.map(versEdit)));
    this.compoRestes.set(c.restes.map(versEdit));
    this.selection.set(null);
  }

  abandonnerComposition(): void {
    this.compoLots.set(null);
    this.compoRestes.set([]);
    this.selection.set(null);
  }

  selectionner(groupe: number, part: PartEdit): void {
    if (this.estSelectionnee(groupe, part)) { this.selection.set(null); return; }
    this.selection.set({ groupe, cle: part.cle });
    this.destination = groupe === NON_PLACES ? NOUVEAU_LOT : NON_PLACES;
    this.montantDeplace = part.montant;
  }

  estSelectionnee(groupe: number, part: PartEdit): boolean {
    const sel = this.selection();
    return !!sel && sel.groupe === groupe && sel.cle === part.cle;
  }

  /** Déplace tout ou partie d'une part vers un autre lot (fractionnement si montant partiel). */
  deplacer(): void {
    const sel = this.selection();
    if (!sel) return;
    const lots = (this.compoLots() ?? []).map(l => [...l]);
    let restes = [...this.compoRestes()];
    const source = sel.groupe === NON_PLACES ? restes : lots[sel.groupe];
    const part = source?.find(p => p.cle === sel.cle);
    if (!part) return;
    const montant = Number(this.montantDeplace);
    if (!montant || montant <= 0 || montant > part.montant + 0.001) {
      this.error.set(`Montant à déplacer invalide (entre 1 et ${part.montant.toLocaleString('fr-FR')} FCFA).`);
      return;
    }
    const destination = Number(this.destination);
    if (destination === sel.groupe) { this.selection.set(null); return; }
    this.error.set('');

    // Retirer de la source, en totalité ou en partie
    const reste = Math.round((part.montant - montant) * 100) / 100;
    const sourceMaj = reste > 0
      ? source.map(p => p.cle === part.cle ? { ...p, montant: reste } : p)
      : source.filter(p => p.cle !== part.cle);
    if (sel.groupe === NON_PLACES) restes = sourceMaj; else lots[sel.groupe] = sourceMaj;

    // Ajouter à la destination, en fusionnant avec la part du même membre
    const ajout = (groupe: PartEdit[]): PartEdit[] => {
      const existante = groupe.find(p => p.membreId === part.membreId);
      return existante
        ? groupe.map(p => p === existante ? { ...p, montant: p.montant + montant } : p)
        : [...groupe, { ...part, cle: this.prochaineCle++, montant }];
    };
    if (destination === NON_PLACES) restes = ajout(restes);
    else if (destination === NOUVEAU_LOT) lots.push(ajout([]));
    else lots[destination] = ajout(lots[destination]);

    this.compoLots.set(lots.filter(l => l.length > 0));
    this.compoRestes.set(restes);
    this.selection.set(null);
  }

  figerComposition(): void {
    const s = this.session();
    const lots = this.compoLots();
    if (!s || !lots || !this.peutFiger()) return;
    if (!confirm(`Figer la session avec ${lots.length} tour(s) ? Les adhésions seront closes et l'ordre des tours tiré au hasard.`)) return;
    this.saving.set(true);
    this.error.set('');
    const corps = lots.map(l => ({ parts: l.map(p => ({ membreId: p.membreId, montant: p.montant })) }));
    this.sessionSvc.figerLot(s.id, corps).subscribe({
      next: l => {
        this.lot.set(l);
        this.abandonnerComposition();
        this.success.set(`Session figée : ${l.nombreTours} tour(s), cagnotte ${(l.cagnotte ?? 0).toLocaleString('fr-FR')} FCFA.`);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur figeage'); this.saving.set(false); },
    });
  }

  // ── Départs en cours de session ───────────────────────────────────────────
  formDepart = signal(false);
  departMembreId = '';
  departDate = new Date().toISOString().slice(0, 10);
  departMotif = '';
  /** Saisie du bureau : obligatoire tant que la session n'est pas figée, sinon facultative. */
  departMois: number | null = null;
  departCotise: number | null = null;
  departRecu: number | null = null;
  /** Montant saisi pour un remboursement, par abandon. */
  remboursements: Record<string, number | null> = {};

  partantsPossibles = computed(() => this.lot()?.membresSession ?? []);
  partant = signal<MembreSessionLot | null>(null);
  /** Dernier tour auquel le membre a cotisé (date d'une séance passée). */
  departSeance = '';

  /**
   * Séances déjà tenues, d'après le calendrier de la session : une date = une séance,
   * même si plusieurs bénéficiaires y ont reçu leur tour.
   */
  seancesPassees = computed(() => {
    const aujourdhui = new Date().toISOString().slice(0, 10);
    const dates = [...new Set((this.lot()?.lots ?? []).map(t => t.dateTour).filter((d): d is string => !!d && d <= aujourdhui))];
    return dates.sort().map((date, i) => ({ numero: i + 1, date }));
  });
  /** Session figée : parts versées ; pas figée : tours de l'ordre payés. */
  tousToursVerses = computed(() => {
    const l = this.lot();
    return !!l && l.lots.length > 0 && l.lots.every(t => t.beneficie);
  });

  /** Membre choisi : reprend ce qu'il a déjà reçu et propose mise × mois pour le cotisé. */
  choisirPartant(): void {
    const m = this.partantsPossibles().find(x => x.membreId === this.departMembreId) ?? null;
    this.partant.set(m);
    this.departRecu = m ? m.dejaRecu : null;
    this.recalculerCotise();
  }

  /** Séance choisie : date du départ et nombre de tours cotisés repris du calendrier. */
  choisirSeance(): void {
    const s = this.seancesPassees().find(x => x.date === this.departSeance);
    if (!s) return;
    this.departDate = s.date;
    this.departMois = s.numero;
    this.recalculerCotise();
  }

  recalculerCotise(): void {
    const mise = this.partant()?.montantMensuel;
    if (mise && this.departMois != null) this.departCotise = mise * this.departMois;
  }

  solde(): number | null {
    if (this.departCotise == null) return null;
    return this.departCotise - (this.departRecu ?? 0);
  }

  declarerDepart(): void {
    const s = this.session();
    const l = this.lot();
    const a = this.partant();
    if (!s || !l || !a) { this.error.set('Choisissez le membre qui part.'); return; }
    if (!l.figee && (this.departMois == null || this.departCotise == null)) {
      this.error.set('Saisissez le nombre de mois et le montant cotisés par le membre.'); return;
    }
    const solde = this.solde();
    const bilan = solde == null ? 'Son solde (cotisé − reçu) sera calculé.'
      : solde < 0 ? `Il doit ${this.fmt(-solde)} FCFA au groupe.`
      : `Avoir gelé de ${this.fmt(solde)} FCFA, rendu en fin de session.`;
    const tours = l.figee ? 'Ses parts non versées seront annulées' : `Ses ${a.toursAVenir} tour(s) non payé(s) seront retirés de l'ordre`;
    if (!confirm(`Enregistrer le départ de ${a.prenom} ${a.nom} ? ${tours} et ses cotisations à venir supprimées. ${bilan}`)) return;
    this.executer(
      this.sessionSvc.declarerAbandonLot(s.id, a.membreId, this.departDate || null, this.departMotif || null,
        // Reçu inchangé : laissé au calcul de l'application
        { moisCotises: this.departMois, totalCotise: this.departCotise,
          totalRecu: this.departRecu === a.dejaRecu ? null : this.departRecu }),
      `Départ de ${a.prenom} ${a.nom} enregistré.`,
      () => this.fermerDepart());
  }

  fermerDepart(): void {
    this.formDepart.set(false);
    this.departMembreId = ''; this.departMotif = '';
    this.departMois = null; this.departCotise = null; this.departRecu = null; this.departSeance = '';
    this.partant.set(null);
  }

  restituer(a: AbandonLot): void {
    const s = this.session();
    if (!s) return;
    if (!confirm(`Restituer ${this.fmt(a.solde)} FCFA à ${a.prenom} ${a.nom} ?`)) return;
    this.executer(this.sessionSvc.restituerAvoirLot(s.id, a.abandonId), `Avoir restitué à ${a.prenom} ${a.nom}.`);
  }

  rembourser(a: AbandonLot): void {
    const s = this.session();
    const montant = Number(this.remboursements[a.abandonId]);
    if (!s) return;
    if (!montant || montant <= 0) { this.error.set('Saisissez le montant remboursé.'); return; }
    this.executer(this.sessionSvc.rembourserDetteLot(s.id, a.abandonId, montant),
      `Remboursement de ${this.fmt(montant)} FCFA enregistré.`,
      () => { this.remboursements[a.abandonId] = null; });
  }

  annulerDepart(a: AbandonLot): void {
    const s = this.session();
    if (!s) return;
    if (!confirm(`Annuler le départ de ${a.prenom} ${a.nom} ? Ses parts ou ses tours seront rétablis ; `
      + `pensez à régénérer ses cotisations à venir.`)) return;
    this.executer(this.sessionSvc.annulerAbandonLot(s.id, a.abandonId), `Départ de ${a.prenom} ${a.nom} annulé.`);
  }

  // ── Tour de remboursement des avoirs gelés ────────────────────────────────
  formRemboursement = signal(false);
  rembDate = '';
  /** Membres cochés et montant versé par chacun, par membreId. */
  rembCoches: Record<string, boolean> = {};
  rembMontants: Record<string, number | null> = {};

  avoirsGeles = computed(() => (this.lot()?.abandons ?? []).filter(a => a.statut === 'AVOIR_GELE'));

  /**
   * Plus grand multiple de leur mise déjà reçu (×12 en général) : ceux qui l'ont atteint
   * sont proposés comme cotisants du tour de remboursement.
   */
  private multipleMax = computed(() => Math.max(0, ...(this.lot()?.membresSession ?? [])
    .map(m => this.multipleRecu(m) ?? 0)));

  multipleRecu(m: MembreSessionLot): number | null {
    return m.montantMensuel && m.dejaRecu > 0 ? Math.round(m.dejaRecu / m.montantMensuel) : null;
  }

  rembTotalCoche(): number {
    return (this.lot()?.membresSession ?? []).filter(m => this.rembCoches[m.membreId])
      .reduce((s, m) => s + (Number(this.rembMontants[m.membreId]) || 0), 0);
  }

  /** Ouvre le formulaire : reprend le tour programmé, sinon propose ceux qui ont reçu le plus gros multiple. */
  ouvrirRemboursement(): void {
    const l = this.lot();
    if (!l) return;
    const r = l.restitution;
    this.rembCoches = {};
    this.rembMontants = {};
    for (const m of l.membresSession) {
      const c = r?.cotisants.find(x => x.membreId === m.membreId);
      this.rembCoches[m.membreId] = r ? !!c : this.multipleMax() > 0 && this.multipleRecu(m) === this.multipleMax();
      this.rembMontants[m.membreId] = c?.montant ?? m.montantMensuel;
    }
    const derniereDate = l.lots.map(t => t.dateTour).filter((d): d is string => !!d).sort().pop();
    this.rembDate = r?.dateTour ?? (derniereDate ? this.moisSuivant(derniereDate) : new Date().toISOString().slice(0, 10));
    this.formRemboursement.set(true);
  }

  private moisSuivant(date: string): string {
    const d = new Date(date + 'T00:00:00');
    d.setMonth(d.getMonth() + 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  programmerRemboursement(): void {
    const s = this.session();
    const l = this.lot();
    if (!s || !l) return;
    const cotisants = l.membresSession.filter(m => this.rembCoches[m.membreId])
      .map(m => ({ membreId: m.membreId, montant: Number(this.rembMontants[m.membreId]) }));
    if (!this.rembDate) { this.error.set('Indiquez la date du tour de remboursement.'); return; }
    if (cotisants.length === 0) { this.error.set('Cochez au moins un membre qui cotise.'); return; }
    if (cotisants.some(c => !c.montant || c.montant <= 0)) {
      this.error.set('Saisissez le montant versé par chaque membre coché.'); return;
    }
    this.executer(this.sessionSvc.programmerRemboursementLot(s.id, this.rembDate, cotisants),
      'Tour de remboursement enregistré.', () => this.formRemboursement.set(false));
  }

  encaisserRemboursement(c: CotisantRemboursementLot, paye: boolean): void {
    const s = this.session();
    if (!s) return;
    if (!paye && !confirm(`Annuler l'encaissement de ${c.prenom} ${c.nom} ?`)) return;
    this.executer(this.sessionSvc.encaisserRemboursementLot(s.id, c.cotisantId, paye),
      paye ? `${this.fmt(c.montant)} FCFA encaissés de ${c.prenom} ${c.nom}.` : 'Encaissement annulé.');
  }

  cloreRemboursement(): void {
    const s = this.session();
    const r = this.lot()?.restitution;
    if (!s || !r) return;
    const ecart = r.ecart < 0 ? ` La trésorerie avance ${this.fmt(-r.ecart)} FCFA.`
      : r.ecart > 0 ? ` La trésorerie garde ${this.fmt(r.ecart)} FCFA.` : '';
    if (!confirm(`Rendre ${this.fmt(r.avoirsARestituer)} FCFA aux ${this.avoirsGeles().length} membre(s) parti(s) ?${ecart}`)) return;
    this.executer(this.sessionSvc.cloreRemboursementLot(s.id), 'Avoirs gelés restitués : tour de remboursement clos.');
  }

  rouvrirRemboursement(): void {
    const s = this.session();
    if (!s) return;
    if (!confirm('Rouvrir le tour de remboursement ? Les avoirs rendus redeviendront gelés.')) return;
    this.executer(this.sessionSvc.rouvrirRemboursementLot(s.id), 'Tour de remboursement rouvert.');
  }

  supprimerRemboursement(): void {
    const s = this.session();
    if (!s) return;
    if (!confirm('Supprimer le tour de remboursement et ses encaissements ?')) return;
    this.executer(this.sessionSvc.supprimerRemboursementLot(s.id), 'Tour de remboursement supprimé.',
      () => this.formRemboursement.set(false));
  }

  private executer(appel: Observable<SessionLotResponse>, message: string, apres?: () => void): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    appel.subscribe({
      next: l => { this.lot.set(l); this.success.set(message); this.saving.set(false); apres?.(); },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Opération impossible'); this.saving.set(false); },
    });
  }

  // ── Cagnotte d'une session pas figée, après des départs ───────────────────
  cagnotteInitiale = computed(() => (this.lot()?.adhesions ?? []).reduce((s, a) => s + (a.montantMensuel ?? 0), 0));
  cagnotteRestante = computed(() => (this.lot()?.adhesions ?? [])
    .filter(a => !a.abandonnee).reduce((s, a) => s + (a.montantMensuel ?? 0), 0));
  totalToursPayes = computed(() => (this.lot()?.lots ?? [])
    .filter(t => t.beneficie).reduce((s, t) => s + (t.montantRecu ?? 0), 0));
  toursAVenir = computed(() => (this.lot()?.lots ?? []).filter(t => !t.beneficie));

  // ── Versements (session figée) ────────────────────────────────────────────
  toursClos = computed(() => (this.lot()?.lots ?? []).filter(t => t.beneficie).length);

  verserPart(p: PartLotMembre): void {
    const s = this.session();
    if (!s) return;
    const retenue = p.montantRetenue ?? 0;
    const detail = retenue > 0
      ? ` (part ${this.fmt(p.partCagnotte)} − fond de caisse ${this.fmt(retenue)})`
      : '';
    if (!confirm(`Verser ${this.fmt(p.montantVerse)} FCFA à ${p.prenom} ${p.nom}${detail} ?`)) return;
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.verserPartLot(s.id, p.partId).subscribe({
      next: l => {
        this.lot.set(l);
        this.success.set(`Part versée à ${p.prenom} ${p.nom}.`);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur de versement'); this.saving.set(false); },
    });
  }

  annulerVersement(p: PartLotMembre): void {
    const s = this.session();
    if (!s) return;
    if (!confirm(`Annuler le versement fait à ${p.prenom} ${p.nom} ? Le tour sera rouvert.`)) return;
    this.saving.set(true);
    this.error.set('');
    this.sessionSvc.annulerVersementPartLot(s.id, p.partId).subscribe({
      next: l => {
        this.lot.set(l);
        this.success.set(`Versement à ${p.prenom} ${p.nom} annulé.`);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur d\'annulation'); this.saving.set(false); },
    });
  }

  private fmt(n: number | null): string {
    return (n ?? 0).toLocaleString('fr-FR', { maximumFractionDigits: 0 });
  }

  totalParts(parts: PartEdit[]): number {
    return parts.reduce((sum, p) => sum + (p.montant || 0), 0);
  }

  ecart(parts: PartEdit[]): number {
    return this.totalParts(parts) - this.montantLot();
  }

  egal(a: number, b: number): boolean {
    return Math.round(a * 100) === Math.round(b * 100);
  }

  totalAdhesions(): number {
    return (this.lot()?.adhesions ?? []).reduce((sum, a) => sum + (a.montantMensuel || 0), 0);
  }
}
