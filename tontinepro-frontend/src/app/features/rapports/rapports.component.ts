import { Component, OnInit, signal, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { MembreService } from '../../core/services/membre.service';
import { RapportService } from '../../core/services/rapport.service';

const MOIS = ['', 'Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin',
              'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

@Component({
  selector: 'app-rapports',
  templateUrl: './rapports.component.html',
})
export class RapportsComponent implements OnInit {
  auth = inject(AuthService);
  private membreSvc  = inject(MembreService);
  private rapportSvc = inject(RapportService);

  tontineId = signal('');
  loading   = signal(true);
  error     = signal('');

  // Rapport mensuel
  moisM  = signal(new Date().getMonth() + 1);
  anneeM = signal(new Date().getFullYear());
  dlM    = signal(false);

  // Export cotisations
  moisC  = signal<number | null>(null);  // null = tous les mois
  anneeC = signal(new Date().getFullYear());
  dlC    = signal(false);

  // Rapport financier
  dlF = signal(false);

  readonly MOIS_OPTIONS = MOIS.slice(1).map((label, i) => ({ value: i + 1, label }));
  readonly ANNEES = Array.from({ length: 4 }, (_, i) => new Date().getFullYear() - i);

  ngOnInit(): void {
    this.membreSvc.getMonProfil().subscribe({
      next:  m => { this.tontineId.set(m.tontineId); this.loading.set(false); },
      error: () => { this.error.set('Impossible de récupérer les informations de la tontine'); this.loading.set(false); },
    });
  }

  downloadMensuel(): void {
    if (!this.tontineId() || this.dlM()) return;
    this.dlM.set(true);
    this.error.set('');
    this.rapportSvc.mensuel(this.tontineId(), this.moisM(), this.anneeM()).subscribe({
      next: blob => {
        this.telecharger(blob, `rapport-mensuel-${pad(this.moisM())}-${this.anneeM()}.pdf`, 'application/pdf');
        this.dlM.set(false);
      },
      error: () => { this.error.set('Erreur lors de la génération du rapport mensuel'); this.dlM.set(false); },
    });
  }

  downloadCotisations(): void {
    if (!this.tontineId() || this.dlC()) return;
    this.dlC.set(true);
    this.error.set('');
    this.rapportSvc.cotisations(this.tontineId(), this.moisC(), this.anneeC()).subscribe({
      next: blob => {
        const nom = this.moisC()
          ? `cotisations-${pad(this.moisC()!)}-${this.anneeC()}.xlsx`
          : `cotisations-${this.anneeC()}.xlsx`;
        this.telecharger(blob, nom, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        this.dlC.set(false);
      },
      error: () => { this.error.set('Erreur lors de la génération de l\'export'); this.dlC.set(false); },
    });
  }

  downloadFinancier(): void {
    if (!this.tontineId() || this.dlF()) return;
    this.dlF.set(true);
    this.error.set('');
    this.rapportSvc.financier(this.tontineId()).subscribe({
      next: blob => {
        this.telecharger(blob, 'rapport-financier.pdf', 'application/pdf');
        this.dlF.set(false);
      },
      error: () => { this.error.set('Erreur lors de la génération du rapport financier'); this.dlF.set(false); },
    });
  }

  moisNom(m: number): string { return MOIS[m] ?? String(m); }

  private telecharger(blob: Blob, filename: string, type: string): void {
    const file = new Blob([blob], { type });
    const url  = URL.createObjectURL(file);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}

function pad(n: number): string { return String(n).padStart(2, '0'); }
