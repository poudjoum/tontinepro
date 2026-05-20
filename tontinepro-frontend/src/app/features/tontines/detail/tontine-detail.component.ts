import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TontineService } from '../../../core/services/tontine.service';
import { DemandeService } from '../../../core/services/demande.service';
import { TontineResponse } from '../../../core/models/tontine.model';
import { DocumentTontineResponse } from '../../../core/models/demande.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-tontine-detail',
  imports: [FormsModule, RouterLink],
  templateUrl: './tontine-detail.component.html',
})
export class TontineDetailComponent implements OnInit {
  private route   = inject(ActivatedRoute);
  private tontSvc = inject(TontineService);
  private demSvc  = inject(DemandeService);

  tontineId = '';
  tontine   = signal<TontineResponse | null>(null);
  documents = signal<DocumentTontineResponse[]>([]);
  loading   = signal(true);
  submitting = signal(false);
  submitted  = signal(false);
  error      = signal('');

  form = { nom: '', prenom: '', email: '', telephone: '', motivation: '' };

  ngOnInit(): void {
    this.tontineId = this.route.snapshot.paramMap.get('id') ?? '';
    this.tontSvc.getById(this.tontineId).subscribe({
      next: t => { this.tontine.set(t); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
    this.demSvc.documentsOfficielsTontine(this.tontineId).subscribe({
      next: docs => this.documents.set(docs),
    });
  }

  soumettre(): void {
    this.submitting.set(true);
    this.error.set('');
    this.demSvc.soumettre(this.tontineId, {
      nom: this.form.nom,
      prenom: this.form.prenom,
      email: this.form.email,
      telephone: this.form.telephone || undefined,
      motivation: this.form.motivation || undefined,
    }).subscribe({
      next: () => { this.submitted.set(true); this.submitting.set(false); },
      error: e => { this.error.set(e.error?.message ?? 'Erreur lors de la soumission'); this.submitting.set(false); },
    });
  }

  urlDoc(docId: string): string {
    return `${environment.apiUrl}/tontines/${this.tontineId}/documents-officiels/${docId}/fichier`;
  }

  labelDoc(type: string): string {
    const map: Record<string, string> = {
      REGLEMENT_INTERIEUR: 'Règlement intérieur',
      STATUTS: 'Statuts',
      AUTRE: 'Autre document',
    };
    return map[type] ?? type;
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
}
