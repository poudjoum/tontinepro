import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { TontineService } from '../../../core/services/tontine.service';
import { SessionService } from '../../../core/services/session.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineResponse, SessionLotResponse, CompositionLotsResponse } from '../../../core/models/tontine.model';
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
  imports: [FormsModule, DecimalPipe, NgTemplateOutlet],
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
