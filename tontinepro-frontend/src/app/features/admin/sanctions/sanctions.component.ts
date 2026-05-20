import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SanctionService } from '../../../core/services/sanction.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineService } from '../../../core/services/tontine.service';
import { SanctionResponse, TypeSanction } from '../../../core/models/sanction.model';
import { TontineResponse } from '../../../core/models/tontine.model';
import { MembreResponse } from '../../../core/models/membre.model';

@Component({
  selector: 'app-sanctions',
  imports: [FormsModule],
  templateUrl: './sanctions.component.html',
})
export class SanctionsComponent implements OnInit {
  private svc     = inject(SanctionService);
  private mbrSvc  = inject(MembreService);
  private tontSvc = inject(TontineService);

  tontines  = signal<TontineResponse[]>([]);
  membres   = signal<MembreResponse[]>([]);
  sanctions = signal<SanctionResponse[]>([]);
  loading   = signal(true);
  saving    = signal(false);
  error     = signal('');
  success   = signal('');
  filtrePayee = signal<boolean | undefined>(undefined);

  tontineId = '';
  form = {
    membreId: '',
    typeSanction: 'AUTRE' as TypeSanction,
    montant: 0,
    motif: '',
  };

  types: { value: TypeSanction; label: string }[] = [
    { value: 'ABSENCE_REUNION',    label: 'Absence à une réunion' },
    { value: 'RETARD_COTISATION',  label: 'Retard de cotisation' },
    { value: 'AUTRE',              label: 'Autre' },
  ];

  total = computed(() =>
    this.sanctions().filter(s => !s.payee).reduce((sum, s) => sum + s.montant, 0)
  );

  ngOnInit(): void {
    this.tontSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        if (list[0]) { this.tontineId = list[0].id; this.charger(); }
        else this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  charger(): void {
    if (!this.tontineId) return;
    this.loading.set(true);
    this.mbrSvc.getAll(this.tontineId).subscribe(m => this.membres.set(m));
    this.svc.lister(this.tontineId, this.filtrePayee()).subscribe({
      next: s => { this.sanctions.set(s); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  creer(): void {
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.svc.creer({ ...this.form }).subscribe({
      next: s => {
        this.sanctions.update(list => [s, ...list]);
        this.success.set('Sanction créée.');
        this.form.membreId = '';
        this.form.montant = 0;
        this.form.motif = '';
        this.saving.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur création');
        this.saving.set(false);
      },
    });
  }

  payer(id: string): void {
    this.svc.marquerPayee(id).subscribe({
      next: updated => {
        this.sanctions.update(list => list.map(s => s.id === id ? updated : s));
      },
    });
  }

  fcfa(n: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  labelType(t: TypeSanction): string {
    return this.types.find(x => x.value === t)?.label ?? t;
  }
}
