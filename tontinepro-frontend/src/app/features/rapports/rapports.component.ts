import { Component, OnInit, signal, inject, effect } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { MembreService } from '../../core/services/membre.service';
import { TontineService } from '../../core/services/tontine.service';
import { TontineContextService } from '../../core/services/tontine-context.service';
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
  private tontineSvc = inject(TontineService);
  private ctx        = inject(TontineContextService);
  private rapportSvc = inject(RapportService);

  constructor() {
    // Rapports de la tontine courante sélectionnée.
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) { this.tontineId.set(id); this.loading.set(false); }
    });
  }

  tontineId = signal('');
  loading   = signal(true);
  error     = signal('');

  // Rapport mensuel
  moisM  = signal(new Date().getMonth() + 1);
  anneeM = signal(new Date().getFullYear());
  dlM    = signal(false);

  // Export cotisations
  moisC  = signal<number | null>(null);
  anneeC = signal(new Date().getFullYear());
  dlC    = signal(false);

  // Rapport financier
  dlF = signal(false);

  // Export sanctions
  payeeS = signal<boolean | undefined>(undefined);
  dlS    = signal(false);

  // Export membres
  dlMbr = signal(false);

  readonly MOIS_OPTIONS = MOIS.slice(1).map((label, i) => ({ value: i + 1, label }));
  readonly ANNEES = Array.from({ length: 4 }, (_, i) => new Date().getFullYear() - i);

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) this.loading.set(false);
  }

  downloadMensuel(): void {
    if (!this.tontineId() || this.dlM()) return;
    this.dlM.set(true); this.error.set('');
    this.rapportSvc.mensuel(this.tontineId(), this.moisM(), this.anneeM()).subscribe({
      next: blob => { this.telecharger(blob, `rapport-mensuel-${pad(this.moisM())}-${this.anneeM()}.pdf`, 'application/pdf'); this.dlM.set(false); },
      error: () => { this.error.set('Erreur rapport mensuel'); this.dlM.set(false); },
    });
  }

  downloadCotisations(): void {
    if (!this.tontineId() || this.dlC()) return;
    this.dlC.set(true); this.error.set('');
    this.rapportSvc.cotisations(this.tontineId(), this.moisC(), this.anneeC()).subscribe({
      next: blob => {
        const nom = this.moisC()
          ? `cotisations-${pad(this.moisC()!)}-${this.anneeC()}.xlsx`
          : `cotisations-${this.anneeC()}.xlsx`;
        this.telecharger(blob, nom, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        this.dlC.set(false);
      },
      error: () => { this.error.set('Erreur export cotisations'); this.dlC.set(false); },
    });
  }

  downloadFinancier(): void {
    if (!this.tontineId() || this.dlF()) return;
    this.dlF.set(true); this.error.set('');
    this.rapportSvc.financier(this.tontineId()).subscribe({
      next: blob => { this.telecharger(blob, 'rapport-financier.pdf', 'application/pdf'); this.dlF.set(false); },
      error: () => { this.error.set('Erreur rapport financier'); this.dlF.set(false); },
    });
  }

  downloadSanctions(): void {
    if (!this.tontineId() || this.dlS()) return;
    this.dlS.set(true); this.error.set('');
    this.rapportSvc.sanctions(this.tontineId(), this.payeeS()).subscribe({
      next: blob => {
        const nom = this.payeeS() === undefined ? 'sanctions-complet.xlsx'
            : this.payeeS() ? 'sanctions-reglees.xlsx' : 'sanctions-impayees.xlsx';
        this.telecharger(blob, nom, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        this.dlS.set(false);
      },
      error: () => { this.error.set('Erreur export sanctions'); this.dlS.set(false); },
    });
  }

  downloadMembres(): void {
    if (!this.tontineId() || this.dlMbr()) return;
    this.dlMbr.set(true); this.error.set('');
    this.rapportSvc.membres(this.tontineId()).subscribe({
      next: blob => { this.telecharger(blob, 'membres.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'); this.dlMbr.set(false); },
      error: () => { this.error.set('Erreur export membres'); this.dlMbr.set(false); },
    });
  }

  moisNom(m: number): string { return MOIS[m] ?? String(m); }

  private telecharger(blob: Blob, filename: string, type: string): void {
    const file = new Blob([blob], { type });
    const url  = URL.createObjectURL(file);
    const a    = document.createElement('a');
    a.href = url; a.download = filename;
    document.body.appendChild(a); a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }
}

function pad(n: number): string { return String(n).padStart(2, '0'); }
