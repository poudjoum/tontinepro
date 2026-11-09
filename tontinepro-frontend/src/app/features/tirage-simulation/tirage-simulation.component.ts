import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { SessionService } from '../../core/services/session.service';
import { TontineService } from '../../core/services/tontine.service';
import { SimulationTirageRequest, SimulationTirageResponse } from '../../core/models/session.model';
import { TontineResponse } from '../../core/models/tontine.model';

@Component({
  selector: 'app-tirage-simulation',
  imports: [FormsModule, CommonModule],
  templateUrl: './tirage-simulation.component.html',
  styleUrls: ['./tirage-simulation.component.css'],
})
export class TirageSimulationComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sessionSvc = inject(SessionService);
  private tontineSvc = inject(TontineService);

  tontineId = '';
  tontine = null as TontineResponse | null;

  dateDebut = new Date().toISOString().split('T')[0]; // aujourd'hui
  loading = false;
  error = '';

  result = null as SimulationTirageResponse | null;

  ngOnInit(): void {
    this.tontineId = this.route.snapshot.paramMap.get('id') ?? '';
    if (!this.tontineId) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.tontineSvc.getById(this.tontineId).subscribe({
      next: t => {
        this.tontine = t;
        // Si la tontine n'est pas CLASSIQUE, on redirige
        if (t.mode !== 'CLASSIQUE') {
          this.error = 'La simulation de tirage au sort n\'est disponible que pour les tontines classiques.';
        }
      },
      error: () => this.router.navigate(['/dashboard']),
    });
  }

  lancerTirage(): void {
    if (!this.tontineId || !this.dateDebut) return;

    if (this.result?.dejaFige) {
      this.error = 'Un tirage a déjà été figé pour cette session. Impossible de relancer.';
      return;
    }

    this.loading = true;
    this.error = '';
    this.result = null;

    const request: SimulationTirageRequest = {
      tontineId: this.tontineId,
      dateDebut: this.dateDebut,
    };

    this.sessionSvc.simulerTirage(request).subscribe({
      next: res => {
        this.result = res;
        this.loading = false;
      },
      error: e => {
        this.error = e.error?.detail ?? e.error?.message ?? 'Erreur lors de la simulation';
        this.loading = false;
      },
    });
  }

  retourner(): void {
    this.router.navigate(['/tontines', this.tontineId, 'sessions']);
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric',
    });
  }
}
