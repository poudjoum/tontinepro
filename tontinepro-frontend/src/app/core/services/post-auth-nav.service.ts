import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Centralize post-authentication routing.
 *
 * - mot de passe temporaire      → /auth/changer-mot-de-passe
 * - SUPER_ADMIN                  → /super-admin
 * - tout autre compte            → /mes-tontines (écran de choix de la tontine)
 *
 * L'écran /mes-tontines liste les tontines du compte (une ou plusieurs) et gère
 * le cas « aucune tontine » (nouveau compte / en attente d'approbation).
 */
@Injectable({ providedIn: 'root' })
export class PostAuthNavService {
  private auth   = inject(AuthService);
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

    // Gestionnaire ou membre : on présente d'abord la liste des tontines.
    this.router.navigate(['/mes-tontines']);
  }
}
