import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TontineService } from '../../../core/services/tontine.service';
import {
  TontineResponse,
  UpdateTontineConfigRequest,
  TypeReglePeriodicite,
} from '../../../core/models/tontine.model';

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

  regles: { value: TypeReglePeriodicite; label: string; avecJour: boolean }[] = [
    { value: 'HEBDOMADAIRE',                   label: 'Hebdomadaire (chaque samedi)',                        avecJour: false },
    { value: 'MENSUEL_JOUR_FIXE',              label: 'Mensuel — jour fixe (ex: le 5 du mois)',              avecJour: true  },
    { value: 'MENSUEL_DERNIER_SAMEDI',         label: 'Mensuel — dernier samedi du mois',                    avecJour: false },
    { value: 'MENSUEL_DERNIER_DIMANCHE',       label: 'Mensuel — dernier dimanche du mois',                  avecJour: false },
    { value: 'MENSUEL_PREMIER_DIMANCHE_APRES', label: 'Mensuel — 1er dimanche après le jour de référence',   avecJour: true  },
    { value: 'MENSUEL_DIMANCHE_SUR_OU_APRES',  label: 'Mensuel — dimanche le jour de référence ou après',    avecJour: true  },
    { value: 'TRIMESTRIEL_JOUR_FIXE',          label: 'Trimestriel — jour fixe',                             avecJour: true  },
    { value: 'SEMESTRIEL_JOUR_FIXE',           label: 'Semestriel — jour fixe',                              avecJour: true  },
    { value: 'ANNUEL_JOUR_FIXE',               label: 'Annuel — jour fixe',                                  avecJour: true  },
    { value: 'DATE_MANUELLE',                  label: 'Date manuelle (fixée par l\'administrateur)',          avecJour: false },
  ];

  get regleSelectionnee() {
    return this.regles.find(r => r.value === this.form.typeReglePeriodicite);
  }

  get afficherJourReference(): boolean {
    return this.regleSelectionnee?.avecJour ?? false;
  }

  get afficherDateManuelle(): boolean {
    return this.form.typeReglePeriodicite === 'DATE_MANUELLE';
  }

  ngOnInit(): void {
    this.svc.getAll().subscribe({
      next: list => {
        const t = list[0];
        if (t) {
          this.tontine.set(t);
          this.form = {
            nom:                  t.nom,
            description:          t.description,
            montantCotisationMin: t.montantCotisationMin,
            montantCotisationMax: t.montantCotisationMax ?? undefined,
            montantConsensuel:    t.montantConsensuel ?? undefined,
            jourReference:        t.jourReference ?? undefined,
            typeReglePeriodicite: t.typeReglePeriodicite,
            dateProchaineTontine: t.dateProchaineTontine ?? undefined,
            tauxInteretPret:      t.tauxInteretPret,
            tauxInteretEpargne:   t.tauxInteretEpargne,
            montantAmende:        t.montantAmende,
            montantPenaliteRetard: t.montantPenaliteRetard,
            montantFondAideAnnuelMembre: t.montantFondAideAnnuelMembre ?? undefined,
            modeContributionAide: t.modeContributionAide,
            montantCotisationAide: t.montantCotisationAide ?? undefined,
          };
        }
        this.loading.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors du chargement');
        this.loading.set(false);
      },
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
