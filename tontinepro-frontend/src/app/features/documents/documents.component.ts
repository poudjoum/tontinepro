import { Component, OnInit, signal, computed, inject, effect, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DocumentService } from '../../core/services/document.service';
import { DemandeService } from '../../core/services/demande.service';
import { AuthService } from '../../core/services/auth.service';
import { MembreService } from '../../core/services/membre.service';
import { TontineContextService } from '../../core/services/tontine-context.service';
import { DocumentResponse, TypeDocument } from '../../core/models/document.model';
import { DocumentTontineResponse, TypeDocumentTontine } from '../../core/models/demande.model';
import { MembreResponse } from '../../core/models/membre.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-documents',
  imports: [FormsModule],
  templateUrl: './documents.component.html',
})
export class DocumentsComponent implements OnInit {
  private svc     = inject(DocumentService);
  private demSvc  = inject(DemandeService);
  auth            = inject(AuthService);
  private mbrSvc  = inject(MembreService);
  private ctx     = inject(TontineContextService);

  constructor() {
    effect(() => {
      const id = this.ctx.tontineCouranteId();
      if (id) untracked(() => this.chargerContexte(id));
    });
  }

  // ── État commun ────────────────────────────────────────────────────────────
  loading   = signal(true);
  uploading = signal(false);
  error     = signal('');
  success   = signal('');

  // ── Vue membre ─────────────────────────────────────────────────────────────
  membreId          = '';
  tontineId         = '';
  documents         = signal<DocumentResponse[]>([]);
  documentsOfficiel = signal<DocumentTontineResponse[]>([]);
  selectedType: TypeDocument = 'CNI';
  selectedFile: File | null = null;

  cni              = computed(() => this.documents().find(d => d.typeDocument === 'CNI') ?? null);
  lettreEngag      = computed(() => this.documents().find(d => d.typeDocument === 'LETTRE_ENGAGEMENT') ?? null);
  planLocalisation = computed(() => this.documents().find(d => d.typeDocument === 'PLAN_LOCALISATION') ?? null);
  reconnDette      = computed(() => this.documents().find(d => d.typeDocument === 'RECONNAISSANCE_DETTE') ?? null);
  autresDocs       = computed(() => this.documents().filter(d =>
    d.typeDocument !== 'CNI' && d.typeDocument !== 'LETTRE_ENGAGEMENT' &&
    d.typeDocument !== 'PLAN_LOCALISATION' && d.typeDocument !== 'RECONNAISSANCE_DETTE'));

  // ── Vue gestionnaire ───────────────────────────────────────────────────────
  membres         = signal<MembreResponse[]>([]);
  selectedMembreId = '';
  docsGest        = signal<DocumentResponse[]>([]);
  loadingGest     = signal(false);

  cniGest        = computed(() => this.docsGest().find(d => d.typeDocument === 'CNI') ?? null);
  lettreGest     = computed(() => this.docsGest().find(d => d.typeDocument === 'LETTRE_ENGAGEMENT') ?? null);
  planGest       = computed(() => this.docsGest().find(d => d.typeDocument === 'PLAN_LOCALISATION') ?? null);
  reconnGest     = computed(() => this.docsGest().find(d => d.typeDocument === 'RECONNAISSANCE_DETTE') ?? null);
  autresGest     = computed(() => this.docsGest().filter(d =>
    d.typeDocument !== 'CNI' && d.typeDocument !== 'LETTRE_ENGAGEMENT' &&
    d.typeDocument !== 'PLAN_LOCALISATION' && d.typeDocument !== 'RECONNAISSANCE_DETTE'));

  readonly types: { value: TypeDocument; label: string }[] = [
    { value: 'CNI',                  label: 'Carte Nationale d\'Identité (CNI)' },
    { value: 'LETTRE_ENGAGEMENT',    label: 'Lettre d\'engagement' },
    { value: 'PLAN_LOCALISATION',    label: 'Plan de localisation (prêt)' },
    { value: 'RECONNAISSANCE_DETTE', label: 'Lettre de reconnaissance de dette (prêt)' },
    { value: 'JUSTIFICATIF_ABSENCE', label: 'Justificatif d\'absence' },
    { value: 'AUTRE',                label: 'Autre document' },
  ];

  ngOnInit(): void {
    this.ctx.init();
  }

  /** (Re)charge le contexte membre/documents pour la tontine courante. */
  private chargerContexte(tontineId: string): void {
    if (this.auth.isGestionnaire()) {
      this.mbrSvc.getAll(tontineId, 'ACTIF').subscribe({
        next: m => { this.membres.set(m); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
      // Charger les docs officiels si le gestionnaire a aussi un profil membre
      this.mbrSvc.getMonProfil(tontineId).subscribe({
        next: m => {
          this.tontineId = m.tontineId;
          this.demSvc.documentsOfficielsTontine(m.tontineId).subscribe({
            next: d => this.documentsOfficiel.set(d),
            error: () => {},
          });
        },
        error: () => {},
      });
    } else {
      this.mbrSvc.getMonProfil(tontineId).subscribe({
        next: m => {
          this.membreId = m.id;
          this.tontineId = m.tontineId;
          this.chargerMesDocs();
          this.demSvc.documentsOfficielsTontine(m.tontineId).subscribe({
            next: d => this.documentsOfficiel.set(d),
            error: () => {},
          });
        },
        error: () => this.loading.set(false),
      });
    }
  }

  // ── Vue membre ─────────────────────────────────────────────────────────────

  chargerMesDocs(): void {
    this.svc.mesDocuments(this.tontineId || undefined).subscribe({
      next: d => { this.documents.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.success.set('');
    this.error.set('');
  }

  upload(): void {
    if (!this.selectedFile || !this.membreId) return;
    this.uploading.set(true);
    this.error.set('');
    this.success.set('');

    this.svc.telecharger(this.membreId, this.selectedType, this.selectedFile).subscribe({
      next: doc => {
        this.documents.update(list => {
          const filtered = list.filter(d => d.typeDocument !== doc.typeDocument);
          return [doc, ...filtered];
        });
        this.success.set('Document chargé avec succès.');
        this.selectedFile = null;
        this.uploading.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors du chargement');
        this.uploading.set(false);
      },
    });
  }

  supprimerMien(id: string): void {
    if (!confirm('Supprimer ce document ?')) return;
    this.svc.supprimer(id).subscribe({
      next: () => this.documents.update(list => list.filter(d => d.id !== id)),
      error: e => this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur suppression'),
    });
  }

  // ── Vue gestionnaire ───────────────────────────────────────────────────────

  onMembreChange(): void {
    if (!this.selectedMembreId) { this.docsGest.set([]); return; }
    this.loadingGest.set(true);
    this.svc.listerParMembre(this.selectedMembreId).subscribe({
      next: d => { this.docsGest.set(d); this.loadingGest.set(false); },
      error: () => this.loadingGest.set(false),
    });
  }

  membreNom(m: MembreResponse): string {
    return `${m.prenom} ${m.nom} (${m.matricule})`;
  }

  // ── Helpers communs ────────────────────────────────────────────────────────

  urlFichier(id: string): string {
    return `${environment.apiUrl}/documents/${id}/fichier`;
  }

  urlDocOfficiel(docId: string): string {
    return `${environment.apiUrl}/tontines/${this.tontineId}/documents-officiels/${docId}/fichier`;
  }

  labelTypeOfficiel(t: TypeDocumentTontine): string {
    const map: Record<TypeDocumentTontine, string> = {
      REGLEMENT_INTERIEUR: 'Règlement intérieur',
      STATUTS: 'Statuts',
      AUTRE: 'Autre document',
    };
    return map[t] ?? t;
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
