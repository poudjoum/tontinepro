import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TontineService } from '../../../core/services/tontine.service';
import { DemandeService } from '../../../core/services/demande.service';
import { SessionService } from '../../../core/services/session.service';
import { MembreService } from '../../../core/services/membre.service';
import { TontineResponse } from '../../../core/models/tontine.model';

interface Etape {
  id: string;
  titre: string;
  desc: string;
  route: string;
  icone: string;
  complete: boolean;
}

@Component({
  selector: 'app-onboarding',
  imports: [RouterLink],
  templateUrl: './onboarding.component.html',
})
export class OnboardingComponent implements OnInit {
  private tontSvc = inject(TontineService);
  private demSvc  = inject(DemandeService);
  private sesSvc  = inject(SessionService);
  private mbrSvc  = inject(MembreService);

  tontine         = signal<TontineResponse | null>(null);
  loading         = signal(true);
  etapes          = signal<Etape[]>([]);

  etapesCompletes = computed(() => this.etapes().filter(e => e.complete).length);
  progression     = computed(() => {
    const total = this.etapes().length;
    return total ? Math.round(this.etapesCompletes() / total * 100) : 0;
  });

  ngOnInit(): void {
    this.tontSvc.getAll().subscribe({
      next: list => {
        const t = list[0];
        if (!t) { this.loading.set(false); return; }
        this.tontine.set(t);
        this.verifierEtapes(t);
      },
      error: () => this.loading.set(false),
    });
  }

  private verifierEtapes(t: TontineResponse): void {
    // Vérifier: documents, membres, session
    Promise.all([
      this.demSvc.documentsOfficielsTontine(t.id).toPromise().catch(() => []),
      this.mbrSvc.getAll(t.id).toPromise().catch(() => []),
      this.sesSvc.listerSessions(t.id).toPromise().catch(() => []),
    ]).then(([docs, membres, sessions]: any[]) => {
      const hasDocs = Array.isArray(docs) && docs.length > 0;
      const hasMembres = Array.isArray(membres) && membres.length >= 2;
      const hasSession = Array.isArray(sessions) && sessions.length > 0;
      const hasBureau = Array.isArray(membres) && membres.some(
        (m: any) => m.fonction !== 'MEMBRE_ORDINAIRE' && m.fonction !== 'SECRETAIRE'
      );

      this.etapes.set([
        {
          id: 'documents',
          titre: 'Publier les documents officiels',
          desc: 'Règlement intérieur et statuts consultables par les candidats.',
          route: '/admin/documents-tontine',
          icone: '📄',
          complete: hasDocs,
        },
        {
          id: 'config',
          titre: 'Configurer la tontine',
          desc: 'Montants min/max, périodicité, fonds d\'aide annuel, amendes.',
          route: '/admin/configuration',
          icone: '⚙️',
          complete: !!(t.montantCotisationMin && t.montantCotisationMin > 0),
        },
        {
          id: 'membres',
          titre: 'Recruter les membres',
          desc: 'Générer un lien d\'invitation ou ajouter des membres directement.',
          route: '/admin/invitations',
          icone: '👥',
          complete: hasMembres,
        },
        {
          id: 'bureau',
          titre: 'Constituer le bureau',
          desc: 'Nommer Président, Trésorier et Censeur parmi les membres inscrits.',
          route: '/admin/membres',
          icone: '🏛️',
          complete: hasBureau,
        },
        {
          id: 'session',
          titre: 'Lancer la première session',
          desc: 'Définir la date de démarrage et la cible de membres.',
          route: '/admin/sessions',
          icone: '🔄',
          complete: hasSession,
        },
      ]);

      this.loading.set(false);
    });
  }
}
