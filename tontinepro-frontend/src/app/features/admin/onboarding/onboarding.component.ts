import { Component, OnInit, signal, inject, computed, effect, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DemandeService } from '../../../core/services/demande.service';
import { SessionService } from '../../../core/services/session.service';
import { MembreService } from '../../../core/services/membre.service';
import { FondsAideService } from '../../../core/services/fonds-aide.service';
import { TontineResponse } from '../../../core/models/tontine.model';
import { TontineContextService } from '../../../core/services/tontine-context.service';

interface Etape {
  id: string;
  titre: string;
  desc: string;
  route: string;
  icone: string;
  complete: boolean;
  /** Étape facultative : affichée mais non comptée dans la progression. */
  optionnel?: boolean;
}

@Component({
  selector: 'app-onboarding',
  imports: [RouterLink],
  templateUrl: './onboarding.component.html',
})
export class OnboardingComponent implements OnInit {
  private demSvc  = inject(DemandeService);
  private sesSvc  = inject(SessionService);
  private mbrSvc  = inject(MembreService);
  private fondsSvc = inject(FondsAideService);

  tontine         = signal<TontineResponse | null>(null);
  loading         = signal(true);
  etapes          = signal<Etape[]>([]);

  etapesRequises  = computed(() => this.etapes().filter(e => !e.optionnel));
  etapesCompletes = computed(() => this.etapesRequises().filter(e => e.complete).length);
  progression     = computed(() => {
    const total = this.etapesRequises().length;
    return total ? Math.round(this.etapesCompletes() / total * 100) : 0;
  });

  private ctx = inject(TontineContextService);

  constructor() {
    // Étapes de la tontine courante (et non de la première de la liste)
    effect(() => {
      const t = this.ctx.tontineCourante();
      if (t) untracked(() => { this.tontine.set(t); this.loading.set(true); this.verifierEtapes(t); });
    });
  }

  ngOnInit(): void {
    this.ctx.init();
    if (!this.ctx.chargement() && !this.ctx.tontineCourante()) this.loading.set(false);
  }

  private verifierEtapes(t: TontineResponse): void {
    // Vérifier: documents, membres, session
    Promise.all([
      this.demSvc.documentsOfficielsTontine(t.id).toPromise().catch(() => []),
      this.mbrSvc.getAll(t.id).toPromise().catch(() => []),
      this.sesSvc.listerSessions(t.id).toPromise().catch(() => []),
      this.fondsSvc.getVersementsAnterieurs(t.id, new Date().getFullYear()).toPromise().catch(() => null),
    ]).then(([docs, membres, sessions, anterieur]: any[]) => {
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
          id: 'fonds-anterieur',
          titre: 'Déclarer le fond de caisse déjà versé',
          desc: 'Si la tontine existait avant l\'application : ce que chaque membre a déjà versé.',
          route: '/admin/fonds-caisse-anterieur',
          icone: '💰',
          complete: !!anterieur && anterieur.total > 0,
          optionnel: true,
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
