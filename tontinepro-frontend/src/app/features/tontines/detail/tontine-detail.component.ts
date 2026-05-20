import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TontineService } from '../../../core/services/tontine.service';
import { DemandeService } from '../../../core/services/demande.service';
import { AuthService } from '../../../core/services/auth.service';
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
  private router  = inject(Router);
  private tontSvc = inject(TontineService);
  private demSvc  = inject(DemandeService);
  auth            = inject(AuthService);

  tontineId  = '';
  tontine    = signal<TontineResponse | null>(null);
  documents  = signal<DocumentTontineResponse[]>([]);
  loading    = signal(true);
  submitting = signal(false);
  submitted  = signal(false);
  error      = signal('');

  // Formulaire rejoindre directement (utilisateur connecté, tontine ouverte)
  rejoindreForm = { nom: '', prenom: '', telephone: '' };
  // Formulaire demande (public, tontine ouverte ou restreinte)
  demandeForm = { nom: '', prenom: '', email: '', telephone: '', motivation: '' };

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

  /** Envoyer une notification aux admins (tontine RESTREINTE) */
  notifierAdmins(): void {
    this.submitting.set(true);
    this.error.set('');
    // Soumet une demande qui déclenchera une notification email aux admins
    const user = this.auth.currentUser();
    this.demSvc.soumettre(this.tontineId, {
      nom: this.rejoindreForm.nom || (user?.email ?? ''),
      prenom: this.rejoindreForm.prenom || '',
      email: user?.email ?? '',
      telephone: this.rejoindreForm.telephone || undefined,
      motivation: 'Demande d\'adhésion à la tontine restreinte',
    }).subscribe({
      next: () => { this.submitted.set(true); this.submitting.set(false); },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de la notification');
        this.submitting.set(false);
      },
    });
  }

  /** Rejoindre directement (connecté + tontine OUVERTE) */
  rejoindreDirectement(): void {
    this.submitting.set(true);
    this.error.set('');
    this.tontSvc.rejoindreOuverte(this.tontineId, this.rejoindreForm).subscribe({
      next: () => {
        this.submitting.set(false);
        this.submitted.set(true);
        // Recharger le dashboard après 2s
        setTimeout(() => this.router.navigate(['/dashboard']), 1500);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de l\'adhésion');
        this.submitting.set(false);
      },
    });
  }

  /** Soumettre une demande (non connecté, ou tontine RESTREINTE) */
  soumettre(): void {
    this.submitting.set(true);
    this.error.set('');
    this.demSvc.soumettre(this.tontineId, {
      nom: this.demandeForm.nom,
      prenom: this.demandeForm.prenom,
      email: this.demandeForm.email,
      telephone: this.demandeForm.telephone || undefined,
      motivation: this.demandeForm.motivation || undefined,
    }).subscribe({
      next: () => { this.submitted.set(true); this.submitting.set(false); },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de la soumission');
        this.submitting.set(false);
      },
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
