import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { DemandeService } from '../../../core/services/demande.service';
import { TontineService } from '../../../core/services/tontine.service';
import { DemandeResponse, StatutDemande } from '../../../core/models/demande.model';
import { TontineResponse } from '../../../core/models/tontine.model';

@Component({
  selector: 'app-demandes',
  imports: [FormsModule, DatePipe],
  templateUrl: './demandes.component.html',
})
export class DemandesComponent implements OnInit {
  private svc     = inject(DemandeService);
  private tontSvc = inject(TontineService);

  tontines  = signal<TontineResponse[]>([]);
  demandes  = signal<DemandeResponse[]>([]);
  loading   = signal(true);
  actif     = signal<string | null>(null);
  error     = signal('');

  tontineId = '';
  filtre: StatutDemande | '' = 'EN_ATTENTE';
  motifRejet = '';
  rejetId: string | null = null;

  ngOnInit(): void {
    this.tontSvc.getAll().subscribe({
      next: list => { this.tontines.set(list); this.charger(); },
      error: () => this.loading.set(false),
    });
  }

  charger(): void {
    this.loading.set(true);
    this.svc.lister(
      this.tontineId || undefined,
      this.filtre || undefined
    ).subscribe({
      next: d => { this.demandes.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  approuver(id: string): void {
    this.actif.set(id);
    this.svc.approuver(id).subscribe({
      next: updated => {
        this.demandes.update(list => list.map(d => d.id === id ? updated : d));
        this.actif.set(null);
      },
      error: e => { this.error.set(e.error?.message ?? 'Erreur'); this.actif.set(null); },
    });
  }

  ouvrirRejet(id: string): void {
    this.rejetId = id;
    this.motifRejet = '';
  }

  confirmerRejet(): void {
    if (!this.rejetId || !this.motifRejet) return;
    const id = this.rejetId;
    this.actif.set(id);
    this.svc.rejeter(id, this.motifRejet).subscribe({
      next: updated => {
        this.demandes.update(list => list.map(d => d.id === id ? updated : d));
        this.rejetId = null;
        this.actif.set(null);
      },
      error: e => { this.error.set(e.error?.message ?? 'Erreur'); this.actif.set(null); },
    });
  }
}
