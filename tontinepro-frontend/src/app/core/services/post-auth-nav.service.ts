import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { MembreService } from './membre.service';

/**
 * Centralize post-authentication routing.
 *
 * - Gestionnaire (ADMIN/SECRETAIRE) → /dashboard
 * - MEMBRE with a member profile    → /dashboard
 * - MEMBRE without a member profile → /tontines  (new user or waiting approval)
 */
@Injectable({ providedIn: 'root' })
export class PostAuthNavService {
  private auth   = inject(AuthService);
  private mbrSvc = inject(MembreService);
  private router = inject(Router);

  navigateAfterLogin(): void {
    // Membre importé avec un mot de passe temporaire : changement obligatoire d'abord
    if (this.auth.mustChangePassword()) {
      this.router.navigate(['/auth/changer-mot-de-passe']);
      return;
    }
    if (this.auth.isSuperAdmin()) {
      this.router.navigate(['/super-admin']);
      return;
    }
    if (this.auth.isGestionnaire()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    // MEMBRE role — check if they have a profile
    this.mbrSvc.getMonProfil().subscribe({
      next:  () => this.router.navigate(['/dashboard']),
      error: () => this.router.navigate(['/tontines']),
    });
  }
}
