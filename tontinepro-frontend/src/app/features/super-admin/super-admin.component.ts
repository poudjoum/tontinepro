import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import {
  SuperAdminService,
  TontinePlatformeResponse,
  ConfigurerRedevanceRequest,
  SuperAdminCreerTontineRequest,
} from '../../core/services/super-admin.service';

@Component({
  selector: 'app-super-admin',
  imports: [FormsModule],
  templateUrl: './super-admin.component.html',
})
export class SuperAdminComponent implements OnInit {
  auth    = inject(AuthService);
  private svc = inject(SuperAdminService);

  tontines   = signal<TontinePlatformeResponse[]>([]);
  loading    = signal(true);
  saving     = signal(false);
  error      = signal('');
  success    = signal('');

  nbActives     = computed(() => this.tontines().filter(t => t.actif).length);
  totalMembres  = computed(() => this.tontines().reduce((s, t) => s + t.nombreMembresActifs, 0));

  // Formulaire création tontine
  afficherFormCreation = signal(false);
  creationForm: SuperAdminCreerTontineRequest = {
    tontineNom: '',
    tontineDescription: '',
    montantCotisationMin: 5000,
    typeAcces: 'OUVERTE',
    secretaireEmail: '',
    secretaireNom: '',
    secretairePrenom: '',
    secretaireTelephone: '',
    secretairePassword: '',
  };
  motDePasseVisible = signal(false);

  // Panneau redevance ouvert pour une tontine
  redevancePanelId = signal<string | null>(null);
  revForm: ConfigurerRedevanceRequest = {
    montant: 0,
    periodicite: 'MENSUEL',
    statut: 'A_JOUR',
    prochainPaiement: undefined,
  };

  ngOnInit(): void {
    this.svc.listerTontines().subscribe({
      next: t => { this.tontines.set(t); this.loading.set(false); },
      error: () => { this.error.set('Impossible de charger les tontines'); this.loading.set(false); },
    });
  }

  ouvrirFormCreation(): void {
    this.afficherFormCreation.set(true);
    this.error.set('');
    this.success.set('');
  }

  annulerCreation(): void {
    this.afficherFormCreation.set(false);
    this.creationForm = {
      tontineNom: '', tontineDescription: '', montantCotisationMin: 5000,
      typeAcces: 'OUVERTE', secretaireEmail: '', secretaireNom: '',
      secretairePrenom: '', secretaireTelephone: '', secretairePassword: '',
    };
  }

  soumettreTontine(): void {
    this.saving.set(true);
    this.error.set('');
    const payload = {
      ...this.creationForm,
      tontineDescription: this.creationForm.tontineDescription || undefined,
      secretaireTelephone: this.creationForm.secretaireTelephone || undefined,
    };
    this.svc.creerTontine(payload).subscribe({
      next: t => {
        this.tontines.update(list => [t, ...list]);
        this.success.set(`Tontine "${t.nom}" créée avec succès.`);
        this.afficherFormCreation.set(false);
        this.annulerCreation();
        this.saving.set(false);
      },
      error: e => { this.error.set(e.message ?? 'Erreur création'); this.saving.set(false); },
    });
  }

  toggleActif(t: TontinePlatformeResponse): void {
    if (!confirm(`${t.actif ? 'Désactiver' : 'Activer'} la tontine "${t.nom}" ?`)) return;
    this.saving.set(true);
    this.svc.toggleActif(t.id, !t.actif).subscribe({
      next: updated => {
        this.tontines.update(list => list.map(x => x.id === updated.id ? updated : x));
        this.success.set(`Tontine "${updated.nom}" ${updated.actif ? 'activée' : 'désactivée'}.`);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur'); this.saving.set(false); },
    });
  }

  ouvrirRedevance(t: TontinePlatformeResponse): void {
    this.redevancePanelId.set(t.id);
    this.revForm = {
      montant: t.redevance?.montant ?? 0,
      periodicite: t.redevance?.periodicite ?? 'MENSUEL',
      statut: t.redevance?.statut ?? 'A_JOUR',
      prochainPaiement: t.redevance?.prochainPaiement ?? undefined,
    };
    this.success.set('');
    this.error.set('');
  }

  sauvegarderRedevance(t: TontinePlatformeResponse): void {
    this.saving.set(true);
    this.svc.configurerRedevance(t.id, this.revForm).subscribe({
      next: updated => {
        this.tontines.update(list => list.map(x => x.id === updated.id ? updated : x));
        this.success.set(`Redevance mise à jour pour "${updated.nom}".`);
        this.redevancePanelId.set(null);
        this.saving.set(false);
      },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur'); this.saving.set(false); },
    });
  }

  reinitialiserMotDePasse(t: TontinePlatformeResponse): void {
    if (!confirm(`Envoyer un lien de réinitialisation au Président et au Secrétaire de "${t.nom}" ?`)) return;
    this.saving.set(true);
    this.error.set('');
    this.svc.reinitialiserMotDePasse(t.id).subscribe({
      next: r => { this.success.set(r.message); this.saving.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur'); this.saving.set(false); },
    });
  }

  logout(): void { this.auth.logout(); }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  statutBadge(s: string | undefined): string {
    switch (s) {
      case 'A_JOUR':    return 'bg-green-100 text-green-700';
      case 'EN_RETARD': return 'bg-red-100 text-red-700';
      case 'GRATUIT':   return 'bg-gray-100 text-gray-600';
      default:          return 'bg-gray-100 text-gray-500';
    }
  }

  statutLabel(s: string | undefined): string {
    switch (s) {
      case 'A_JOUR':    return 'À jour';
      case 'EN_RETARD': return 'En retard';
      case 'GRATUIT':   return 'Gratuit';
      default:          return '—';
    }
  }
}
