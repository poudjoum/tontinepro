import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import * as XLSX from 'xlsx';
import { TontineService } from '../../../core/services/tontine.service';
import { SessionService } from '../../../core/services/session.service';
import { TontineResponse } from '../../../core/models/tontine.model';
import { SessionResponse } from '../../../core/models/session.model';
import { environment } from '../../../../environments/environment';

interface ImportPayload {
  fondAnterieurSolde: number | null;
  partEpargneParMembre: number | null;
  toursPasses: any[];
  cotisations: any[];
  absencesSanctions: any[];
  epargnesComplementaires: any[];
  prets: any[];
}

@Component({
  selector: 'app-historique-import',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './historique-import.component.html',
})
export class HistoriqueImportComponent implements OnInit {
  private http       = inject(HttpClient);
  private tontineSvc = inject(TontineService);
  private sessionSvc = inject(SessionService);

  tontines  = signal<TontineResponse[]>([]);
  sessions  = signal<SessionResponse[]>([]);
  loading   = signal(false);
  importing = signal(false);
  error     = signal('');
  success   = signal('');

  tontineId = '';
  sessionId = '';
  fileName  = '';

  parsed   = signal<ImportPayload | null>(null);
  resultat = signal<any>(null);

  ngOnInit(): void {
    this.tontineSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        if (list[0]) { this.tontineId = list[0].id; this.chargerSessions(); }
      },
    });
  }

  chargerSessions(): void {
    if (!this.tontineId) return;
    this.sessionSvc.listerSessions(this.tontineId).subscribe({
      next: list => {
        this.sessions.set(list);
        const enCours = list.find(s => s.statut === 'EN_COURS');
        this.sessionId = enCours?.id ?? list[0]?.id ?? '';
      },
    });
  }

  // ── Télécharger le modèle ─────────────────────────────────────────────

  telechargerModele(): void {
    const wb = XLSX.utils.book_new();

    XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet([
      ['Champ', 'Valeur', 'Description'],
      ['fond_anterieur_solde', 0, 'Solde caisse trésorier (FCFA)'],
      ['part_epargne_par_membre', 0, 'Capital épargne alloué par membre (FCFA)'],
    ]), 'SESSION');

    XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet([
      ['telephone', 'date', 'montant_recu'],
      ['699001122', '2025-11-10', 250000],
    ]), 'TOURS_PASSES');

    XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet([
      ['telephone', 'mois', 'annee', 'montant_cotisation', 'montant_fond_aide',
       'cotisation_statut', 'fond_aide_statut'],
      ['699001122', 11, 2025, 25000, 5000, 'PAYEE', 'PAYEE'],
      ['677334455', 11, 2025, 25000, 5000, 'PAYEE', 'EN_RETARD'],
    ]), 'COTISATIONS_PASSEES');

    XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet([
      ['telephone', 'date_reunion', 'justifiee', 'sanction_montant', 'sanction_payee'],
      ['677334455', '2025-11-10', 'NON', 2000, 'NON'],
      ['699001122', '2025-12-10', 'OUI', 0, ''],
    ]), 'ABSENCES_SANCTIONS');

    XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet([
      ['telephone', 'montant', 'date'],
      ['699001122', 10000, '2026-01-15'],
    ]), 'EPARGNE_COMPLEMENTAIRE');

    XLSX.utils.book_append_sheet(wb, XLSX.utils.aoa_to_sheet([
      ['telephone', 'montant', 'date_pret', 'duree_mois', 'montant_rembourse'],
      ['677334455', 50000, '2026-01-15', 3, 0],
    ]), 'PRETS_EN_COURS');

    XLSX.writeFile(wb, 'modele_historique_tontine.xlsx');
  }

  // ── Lire le fichier Excel ─────────────────────────────────────────────

  onFichierChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.fileName = file.name;
    this.error.set('');
    this.resultat.set(null);
    this.parsed.set(null);

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = new Uint8Array(e.target!.result as ArrayBuffer);
        const wb = XLSX.read(data, { type: 'array' });
        this.parsed.set(this.parseWorkbook(wb));
      } catch (err: any) {
        this.error.set('Erreur lors de la lecture du fichier : ' + err.message);
      }
    };
    reader.readAsArrayBuffer(file);
  }

  private parseWorkbook(wb: XLSX.WorkBook): ImportPayload {
    const session   = this.sheetToRows(wb, 'SESSION');
    const sessionMap: Record<string, any> = {};
    session.forEach(r => { if (r['Champ']) sessionMap[r['Champ']] = r['Valeur']; });

    return {
      fondAnterieurSolde:    this.num(sessionMap['fond_anterieur_solde']),
      partEpargneParMembre:  this.num(sessionMap['part_epargne_par_membre']),
      toursPasses:           this.sheetToRows(wb, 'TOURS_PASSES').map(r => ({
        telephone:   String(r['telephone'] ?? ''),
        date:        this.formatDate(r['date']),
        montantRecu: this.num(r['montant_recu']),
      })),
      cotisations:           this.sheetToRows(wb, 'COTISATIONS_PASSEES').map(r => ({
        telephone:          String(r['telephone'] ?? ''),
        mois:               this.num(r['mois']),
        annee:              this.num(r['annee']),
        montantCotisation:  this.num(r['montant_cotisation']),
        montantFondAide:    this.num(r['montant_fond_aide']),
        cotisationStatut:   String(r['cotisation_statut'] ?? 'PAYEE').toUpperCase(),
        fondAideStatut:     String(r['fond_aide_statut'] ?? 'PAYEE').toUpperCase(),
      })),
      absencesSanctions:     this.sheetToRows(wb, 'ABSENCES_SANCTIONS').map(r => ({
        telephone:       String(r['telephone'] ?? ''),
        dateReunion:     this.formatDate(r['date_reunion']),
        justifiee:       String(r['justifiee'] ?? 'NON').toUpperCase() === 'OUI',
        sanctionMontant: this.num(r['sanction_montant']),
        sanctionPayee:   String(r['sanction_payee'] ?? 'NON').toUpperCase() === 'OUI',
      })),
      epargnesComplementaires: this.sheetToRows(wb, 'EPARGNE_COMPLEMENTAIRE').map(r => ({
        telephone: String(r['telephone'] ?? ''),
        montant:   this.num(r['montant']),
        date:      this.formatDate(r['date']),
      })),
      prets:                 this.sheetToRows(wb, 'PRETS_EN_COURS').map(r => ({
        telephone:        String(r['telephone'] ?? ''),
        montant:          this.num(r['montant']),
        datePret:         this.formatDate(r['date_pret']),
        dureeMois:        this.num(r['duree_mois']),
        montantRembourse: this.num(r['montant_rembourse']),
      })),
    };
  }

  private sheetToRows(wb: XLSX.WorkBook, name: string): any[] {
    const ws = wb.Sheets[name];
    if (!ws) return [];
    return XLSX.utils.sheet_to_json(ws, { defval: null });
  }

  private num(v: any): number {
    const n = parseFloat(String(v ?? 0));
    return isNaN(n) ? 0 : n;
  }

  private formatDate(v: any): string {
    if (!v) return '';
    if (typeof v === 'number') {
      // Excel serial date
      const d = XLSX.SSF.parse_date_code(v);
      return `${d.y}-${String(d.m).padStart(2,'0')}-${String(d.d).padStart(2,'0')}`;
    }
    const s = String(v);
    // Try dd/mm/yyyy
    const parts = s.split('/');
    if (parts.length === 3 && parts[2].length === 4) {
      return `${parts[2]}-${parts[1].padStart(2,'0')}-${parts[0].padStart(2,'0')}`;
    }
    return s; // already yyyy-MM-dd
  }

  // ── Importer ──────────────────────────────────────────────────────────

  importer(): void {
    if (!this.sessionId || !this.parsed()) return;
    this.importing.set(true);
    this.error.set('');
    this.resultat.set(null);

    this.http.post(
      `${environment.apiUrl}/sessions/${this.sessionId}/historique/importer`,
      this.parsed()
    ).subscribe({
      next: (res: any) => {
        this.resultat.set(res);
        this.success.set('Import réalisé avec succès.');
        this.importing.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de l\'import');
        this.importing.set(false);
      },
    });
  }
}