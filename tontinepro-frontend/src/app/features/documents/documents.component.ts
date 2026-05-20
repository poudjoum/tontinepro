import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DocumentService } from '../../core/services/document.service';
import { AuthService } from '../../core/services/auth.service';
import { MembreService } from '../../core/services/membre.service';
import { DocumentResponse, TypeDocument } from '../../core/models/document.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-documents',
  imports: [FormsModule],
  templateUrl: './documents.component.html',
})
export class DocumentsComponent implements OnInit {
  private svc     = inject(DocumentService);
  private auth    = inject(AuthService);
  private mbrSvc  = inject(MembreService);

  documents = signal<DocumentResponse[]>([]);
  loading   = signal(true);
  uploading = signal(false);
  error     = signal('');
  success   = signal('');

  membreId     = '';
  selectedType: TypeDocument = 'CNI';
  selectedFile: File | null = null;

  types: { value: TypeDocument; label: string }[] = [
    { value: 'CNI',               label: 'Carte Nationale d\'Identité' },
    { value: 'LETTRE_ENGAGEMENT', label: 'Lettre d\'engagement (prêt)' },
    { value: 'JUSTIFICATIF_ABSENCE', label: 'Justificatif d\'absence' },
    { value: 'AUTRE',             label: 'Autre document' },
  ];

  ngOnInit(): void {
    if (this.auth.isAdmin()) {
      this.loading.set(false);
    } else {
      this.mbrSvc.getMonProfil().subscribe({
        next: m => { this.membreId = m.id; this.charger(); },
        error: () => this.loading.set(false),
      });
    }
  }

  charger(): void {
    if (!this.membreId && !this.auth.isAdmin()) return;
    const obs = this.auth.isAdmin() && this.membreId
      ? this.svc.listerParMembre(this.membreId)
      : this.svc.mesDocuments();

    obs.subscribe({
      next: d => { this.documents.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  upload(): void {
    if (!this.selectedFile) return;
    const id = this.membreId;
    if (!id) return;
    this.uploading.set(true);
    this.error.set('');

    this.svc.telecharger(id, this.selectedType, this.selectedFile).subscribe({
      next: doc => {
        this.documents.update(list => [doc, ...list]);
        this.success.set('Document téléchargé avec succès.');
        this.selectedFile = null;
        this.uploading.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors du téléchargement');
        this.uploading.set(false);
      },
    });
  }

  supprimer(id: string): void {
    if (!confirm('Supprimer ce document ?')) return;
    this.svc.supprimer(id).subscribe({
      next: () => this.documents.update(list => list.filter(d => d.id !== id)),
      error: e => this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur suppression'),
    });
  }

  urlFichier(id: string): string {
    return `${environment.apiUrl}/documents/${id}/fichier`;
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  labelType(t: TypeDocument): string {
    return this.types.find(x => x.value === t)?.label ?? t;
  }
}
