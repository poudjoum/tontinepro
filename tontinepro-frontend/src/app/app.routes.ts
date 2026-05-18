import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

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
      { path: '', redirectTo: 'login', pathMatch: 'full' },
    ],
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
    ],
  },

  { path: '**', redirectTo: 'dashboard' },
];
