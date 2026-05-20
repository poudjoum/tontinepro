import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AbsenceService } from '../../../core/services/absence.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineService } from '../../../core/services/tontine.service';
import { AbsenceResponse, AppelPresenceResult } from '../../../core/models/absence.model';
import { MembreResponse } from '../../../core/models/membre.model';
import { TontineResponse } from '../../../core/models/tontine.model';

type Mode = 'absence' | 'appel';

@Component({
  selector: 'app-absences',
  imports: [FormsModule],
  templateUrl: './absences.component.html',
})
export class AbsencesComponent implements OnInit {
  private absSvc  = inject(AbsenceService);
  private mbrSvc  = inject(MembreService);
  private tontSvc = inject(TontineService);

  tontines  = signal<TontineResponse[]>([]);
  membres   = signal<MembreResponse[]>([]);
  absences  = signal<AbsenceResponse[]>([]);
  loading   = signal(true);
  saving    = signal(false);
  error     = signal('');
  success   = signal('');
  mode      = signal<Mode>('appel');

  tontineId  = '';

  // Formulaire absence individuelle
  form = {
    membreId: '',
    dateReunion: new Date().toISOString().split('T')[0],
    justifiee: false,
    motif: '',
  };

  // Appel de présence groupé
  appelDate       = new Date().toISOString().split('T')[0];
  presentsSet     = new Set<string>();
  appelResult     = signal<AppelPresenceResult | null>(null);

  ngOnInit(): void {
    this.tontSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        if (list[0]) {
          this.tontineId = list[0].id;
          this.charger();
        } else {
          this.loading.set(false);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  charger(): void {
    if (!this.tontineId) return;
    this.loading.set(true);
    this.mbrSvc.getAll(this.tontineId, 'ACTIF').subscribe(m => {
      this.membres.set(m);
      // Par défaut : tous présents
      this.presentsSet = new Set(m.map(x => x.id));
    });
    this.absSvc.lister(this.tontineId).subscribe({
      next: a => { this.absences.set(a); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  // ── Absence individuelle ──────────────────────────────────────────────

  enregistrer(): void {
    this.saving.set(true);
    this.error.set(''); this.success.set('');
    this.absSvc.enregistrer({ ...this.form }).subscribe({
      next: abs => {
        this.absences.update(list => [abs, ...list]);
        this.success.set(`Absence enregistrée${!this.form.justifiee ? ' — sanction générée automatiquement' : ''}.`);
        this.form.membreId = ''; this.form.motif = '';
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur');
        this.saving.set(false);
      },
    });
  }

  // ── Appel de présence groupé ──────────────────────────────────────────

  togglePresent(membreId: string): void {
    if (this.presentsSet.has(membreId)) {
      this.presentsSet.delete(membreId);
    } else {
      this.presentsSet.add(membreId);
    }
  }

  estPresent(membreId: string): boolean {
    return this.presentsSet.has(membreId);
  }

  tousPresents(): void {
    this.presentsSet = new Set(this.membres().map(m => m.id));
  }

  tousAbsents(): void {
    this.presentsSet = new Set();
  }

  lancerAppel(): void {
    this.saving.set(true);
    this.error.set(''); this.success.set(''); this.appelResult.set(null);
    this.absSvc.appelPresence({
      tontineId: this.tontineId,
      dateReunion: this.appelDate,
      membresPresentsIds: [...this.presentsSet],
    }).subscribe({
      next: result => {
        this.appelResult.set(result);
        this.charger();
        this.success.set(
          `Appel terminé : ${result.presentsCount} présent(s), ${result.absentsCount} absent(s), ${result.sanctionsGenerees} sanction(s) générée(s).`
        );
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de l\'appel');
        this.saving.set(false);
      },
    });
  }

  nomMembre(id: string): string {
    const m = this.membres().find(m => m.id === id);
    return m ? `${m.prenom} ${m.nom}` : id;
  }
}
