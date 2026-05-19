import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AbsenceService } from '../../../core/services/absence.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineService } from '../../../core/services/tontine.service';
import { AbsenceResponse } from '../../../core/models/absence.model';
import { MembreResponse } from '../../../core/models/membre.model';
import { TontineResponse } from '../../../core/models/tontine.model';

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

  tontineId  = '';
  form = {
    membreId: '',
    dateReunion: new Date().toISOString().split('T')[0],
    justifiee: false,
    motif: '',
  };

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
    this.mbrSvc.getAll(this.tontineId).subscribe(m => this.membres.set(m));
    this.absSvc.lister(this.tontineId).subscribe({
      next: a => { this.absences.set(a); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  enregistrer(): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    this.absSvc.enregistrer({ ...this.form }).subscribe({
      next: abs => {
        this.absences.update(list => [abs, ...list]);
        this.success.set(`Absence enregistrée${!this.form.justifiee ? ' — sanction générée automatiquement' : ''}.`);
        this.form.membreId = '';
        this.form.motif = '';
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors de l\'enregistrement');
        this.saving.set(false);
      },
    });
  }

  nomMembre(id: string): string {
    const m = this.membres().find(m => m.id === id);
    return m ? `${m.prenom} ${m.nom}` : id;
  }
}
