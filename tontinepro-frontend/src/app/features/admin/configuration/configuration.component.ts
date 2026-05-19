import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TontineService } from '../../../core/services/tontine.service';
import { TontineResponse, UpdateTontineConfigRequest, PeriodeCotisation } from '../../../core/models/tontine.model';

@Component({
  selector: 'app-configuration',
  imports: [FormsModule],
  templateUrl: './configuration.component.html',
})
export class ConfigurationComponent implements OnInit {
  private svc = inject(TontineService);

  tontine  = signal<TontineResponse | null>(null);
  loading  = signal(true);
  saving   = signal(false);
  success  = signal('');
  error    = signal('');

  form: UpdateTontineConfigRequest = {};

  periodes: { value: PeriodeCotisation; label: string }[] = [
    { value: 'HEBDOMADAIRE', label: 'Hebdomadaire' },
    { value: 'MENSUEL',      label: 'Mensuel' },
    { value: 'BIMENSUEL',    label: 'Bimensuel (tous les 2 mois)' },
    { value: 'TRIMESTRIEL',  label: 'Trimestriel' },
    { value: 'SEMESTRIEL',   label: 'Semestriel' },
    { value: 'ANNUEL',       label: 'Annuel' },
  ];

  ngOnInit(): void {
    this.svc.getAll().subscribe({
      next: list => {
        const t = list[0];
        if (t) {
          this.tontine.set(t);
          this.form = {
            nom: t.nom,
            description: t.description,
            montantCotisation: t.montantCotisation,
            jourCotisation: t.jourCotisation,
            periodeCotisation: t.periodeCotisation,
            tauxInteretPret: t.tauxInteretPret,
            tauxInteretEpargne: t.tauxInteretEpargne,
            montantAmende: t.montantAmende,
            montantPenaliteRetard: t.montantPenaliteRetard,
            modeContributionAide: t.modeContributionAide,
            montantCotisationAide: t.montantCotisationAide ?? undefined,
          };
        }
        this.loading.set(false);
      },
      error: e => { this.error.set(e.error?.message ?? 'Erreur chargement'); this.loading.set(false); },
    });
  }

  sauvegarder(): void {
    const t = this.tontine();
    if (!t) return;
    this.saving.set(true);
    this.success.set('');
    this.error.set('');

    this.svc.updateConfig(t.id, this.form).subscribe({
      next: updated => {
        this.tontine.set(updated);
        this.success.set('Configuration enregistrée avec succès.');
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors de la sauvegarde');
        this.saving.set(false);
      },
    });
  }
}
