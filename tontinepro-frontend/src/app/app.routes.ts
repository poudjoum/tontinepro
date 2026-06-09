import { Routes } from '@angular/router';
import { authGuard, adminGuard, superAdminGuard } from './core/guards/auth.guard';
import { notConfiguredGuard } from './core/guards/setup.guard';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [

  // Landing (page d'accueil publique)
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/landing/landing.component').then(m => m.LandingComponent),
  },

  // Setup — accessible seulement si la plateforme n'est pas encore configurée
  {
    path: 'setup',
    canActivate: [notConfiguredGuard],
    loadComponent: () =>
      import('./features/setup/setup.component').then(m => m.SetupComponent),
  },

  // Création libre-service d'une tontine
  {
    path: 'creer-tontine',
    loadComponent: () =>
      import('./features/creer-tontine/creer-tontine.component').then(m => m.CreerTontineComponent),
  },

  // Liste publique des tontines disponibles
  {
    path: 'tontines',
    loadComponent: () =>
      import('./features/tontines/tontines.component').then(m => m.TontinesComponent),
  },
  {
    path: 'tontines/:id',
    loadComponent: () =>
      import('./features/tontines/detail/tontine-detail.component').then(m => m.TontineDetailComponent),
  },

  // Rejoindre via lien d'invitation (public)
  {
    path: 'rejoindre/:token',
    loadComponent: () =>
      import('./features/rejoindre/rejoindre.component').then(m => m.RejoindreComponent),
  },

  // Auth (sans layout)
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login.component').then(m => m.LoginComponent),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register/register.component').then(m => m.RegisterComponent),
      },
      {
        path: '2fa',
        loadComponent: () =>
          import('./features/auth/two-fa/two-fa.component').then(m => m.TwoFaComponent),
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
      },
      {
        path: 'mot-de-passe-oublie',
        loadComponent: () =>
          import('./features/auth/mot-de-passe-oublie/mot-de-passe-oublie.component').then(m => m.MotDePasseOublieComponent),
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ],
  },

  // Super Admin — interface plateforme (sans shell tontine)
  {
    path: 'super-admin',
    canActivate: [superAdminGuard],
    loadComponent: () =>
      import('./features/super-admin/super-admin.component').then(m => m.SuperAdminComponent),
  },

  // App protégée — avec Shell (header + bottom-nav)
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'cotisations',
        loadComponent: () =>
          import('./features/cotisations/cotisations.component').then(m => m.CotisationsComponent),
      },
      {
        path: 'epargne',
        loadComponent: () =>
          import('./features/epargne/epargne.component').then(m => m.EpargneComponent),
      },
      {
        path: 'prets',
        loadComponent: () =>
          import('./features/prets/prets.component').then(m => m.PretsComponent),
      },
      {
        path: 'aides',
        loadComponent: () =>
          import('./features/aides/aides.component').then(m => m.AidesComponent),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import('./features/notifications/notifications.component').then(m => m.NotificationsComponent),
      },
      {
        path: 'suivi',
        loadComponent: () =>
          import('./features/suivi/suivi.component').then(m => m.SuiviComponent),
      },
      {
        path: 'sanctions',
        loadComponent: () =>
          import('./features/sanctions/sanctions.component').then(m => m.SanctionsComponent),
      },
      {
        path: 'mon-tour',
        loadComponent: () =>
          import('./features/mon-tour/mon-tour.component').then(m => m.MonTourComponent),
      },
      {
        path: 'profil',
        loadComponent: () =>
          import('./features/profil/profil.component').then(m => m.ProfilComponent),
      },
      {
        path: 'rapports',
        loadComponent: () =>
          import('./features/rapports/rapports.component').then(m => m.RapportsComponent),
      },
      {
        path: 'documents',
        loadComponent: () =>
          import('./features/documents/documents.component').then(m => m.DocumentsComponent),
      },
      {
        path: 'rapport-tour/:sessionId/:ordreBeneficiaireId',
        loadComponent: () =>
          import('./features/rapport-tour/rapport-tour.component').then(m => m.RapportTourComponent),
      },
      {
        path: 'rapport-fin-session/:sessionId',
        loadComponent: () =>
          import('./features/rapport-fin-session/rapport-fin-session.component').then(m => m.RapportFinSessionComponent),
      },

      // Administration (admin only)
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/admin/admin.component').then(m => m.AdminComponent),
          },
          {
            path: 'configuration',
            loadComponent: () =>
              import('./features/admin/configuration/configuration.component').then(m => m.ConfigurationComponent),
          },
          {
            path: 'invitations',
            loadComponent: () =>
              import('./features/admin/invitations/invitations.component').then(m => m.InvitationsComponent),
          },
          {
            path: 'absences',
            loadComponent: () =>
              import('./features/admin/absences/absences.component').then(m => m.AbsencesComponent),
          },
          {
            path: 'sanctions',
            loadComponent: () =>
              import('./features/admin/sanctions/sanctions.component').then(m => m.SanctionsComponent),
          },
          {
            path: 'membres',
            loadComponent: () =>
              import('./features/admin/membres/membres-admin.component').then(m => m.MembresAdminComponent),
          },
          {
            path: 'demandes',
            loadComponent: () =>
              import('./features/admin/demandes/demandes.component').then(m => m.DemandesComponent),
          },
          {
            path: 'sessions',
            loadComponent: () =>
              import('./features/admin/sessions/sessions.component').then(m => m.SessionsComponent),
          },
          {
            path: 'documents-tontine',
            loadComponent: () =>
              import('./features/admin/documents-tontine/documents-tontine.component').then(m => m.DocumentsTontineComponent),
          },
          {
            path: 'onboarding',
            loadComponent: () =>
              import('./features/admin/onboarding/onboarding.component').then(m => m.OnboardingComponent),
          },
          {
            path: 'historique-import',
            loadComponent: () =>
              import('./features/admin/historique-import/historique-import.component').then(m => m.HistoriqueImportComponent),
          },
          {
            path: 'reprise-session',
            loadComponent: () =>
              import('./features/admin/reprise-session/reprise-session.component').then(m => m.RepriseSessionComponent),
          },
          {
            path: 'tontine-lot',
            loadComponent: () =>
              import('./features/admin/tontine-lot/tontine-lot.component').then(m => m.TontineLotComponent),
          },
        ],
      },
    ],
  },

  { path: '**', redirectTo: '' },
];
