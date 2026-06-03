import { Component, OnInit, signal, inject, effect, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DemandeService } from '../../../core/services/demande.service';
import { TontineService } from '../../../core/services/tontine.service';
import { TontineContextService } from '../../../core/services/tontine-context.service';
import { DocumentTontineResponse, TypeDocumentTontine } from '../../../core/models/demande.model';
import { TontineResponse } from '../../../core/models/tontine.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-documents-tontine',
  imports: [FormsModule],
  templateUrl: './documents-tontine.component.html',
})
export class DocumentsTontineComponent implements OnInit {
  private svc     = inject(DemandeService);
  private tontSvc = inject(TontineService);
  private ctx     = inject(TontineContextService);

  tontines  = signal<TontineResponse[]>([]);
  documents = signal<DocumentTontineResponse[]>([]);
  loading   = signal(true);
  uploading = signal(false);
  error     = signal('');
  success   = signal('');

  tontineId    = '';
  selectedType: TypeDocumentTontine = 'REGLEMENT_INTERIEUR';
  selectedFile: File | null = null;

  types: { value: TypeDocumentTontine; label: string; desc: string }[] = [
    { value: 'REGLEMENT_INTERIEUR', label: 'Règlement intérieur',
      desc: 'Règles de fonctionnement de la tontine' },
    { value: 'STATUTS', label: 'Statuts',
      desc: 'Statuts officiels de l\'association' },
    { value: 'AUTRE', label: 'Autre document',
      desc: 'Tout autre document utile aux membres' },
  ];

  constructor() {
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      this.tontines.set(this.ctx.tontines());
      if (id) { this.tontineId = id; untracked(() => this.charger()); }
    });
  }

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.tontineCouranteId()) this.loading.set(false);
  }

  charger(): void {
    if (!this.tontineId) return;
    this.svc.documentsOfficielsTontine(this.tontineId).subscribe({
      next: docs => { this.documents.set(docs); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onFile(event: Event): void {
    this.selectedFile = (event.target as HTMLInputElement).files?.[0] ?? null;
  }

  upload(): void {
    if (!this.selectedFile || !this.tontineId) return;
    this.uploading.set(true);
    this.error.set('');
    this.svc.uploadDocumentOfficiel(this.tontineId, this.selectedType, this.selectedFile).subscribe({
      next: doc => {
        this.documents.update(list => [doc, ...list]);
        this.success.set('Document téléchargé avec succès.');
        this.selectedFile = null;
        this.uploading.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur téléchargement');
        this.uploading.set(false);
      },
    });
  }

  supprimer(docId: string): void {
    if (!confirm('Supprimer ce document ?')) return;
    this.svc.supprimerDocumentOfficiel(this.tontineId, docId).subscribe({
      next: () => this.documents.update(list => list.filter(d => d.id !== docId)),
      error: e => this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur suppression'),
    });
  }

  urlFichier(docId: string): string {
    return `${environment.apiUrl}/tontines/${this.tontineId}/documents-officiels/${docId}/fichier`;
  }

  labelType(t: TypeDocumentTontine): string {
    return this.types.find(x => x.value === t)?.label ?? t;
  }

  formatSize(b: number): string {
    return b < 1048576 ? `${(b / 1024).toFixed(1)} Ko` : `${(b / 1048576).toFixed(1)} Mo`;
  }
}
