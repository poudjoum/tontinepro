import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { MembreService } from '../services/membre.service';
import { TontineContextService } from '../services/tontine-context.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn() ? true : router.createUrlTree(['/auth/login']);
};

/**
 * Force le changement du mot de passe temporaire (membres importés) avant
 * d'accéder à l'application. Tant que le drapeau est levé, on redirige vers
 * l'écran de changement de mot de passe.
 */
export const mustChangePasswordGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn() && auth.mustChangePassword()) {
    return router.createUrlTree(['/auth/changer-mot-de-passe']);
  }
  return true;
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isGestionnaire()) return true;
  return router.createUrlTree(['/dashboard']);
};

export const superAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isSuperAdmin()) return true;
  return router.createUrlTree([auth.isLoggedIn() ? '/dashboard' : '/auth/login']);
};

/**
 * Accès aux écrans d'encaissement (collecte des parts d'aide).
 *
 * L'encaissement revient au Trésorier de la tontine, dont le compte a le plus
 * souvent le rôle MEMBRE : `adminGuard` ne convient donc pas. La fonction dépend
 * de la tontine courante, elle n'est pas dans le jeton — on interroge le profil
 * membre. Le backend reste seul juge (`sec.peutEncaisser`) ; ce garde ne fait
 * qu'éviter d'ouvrir un écran qui répondrait 403.
 */
export const encaissementGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const membres = inject(MembreService);
  const ctx = inject(TontineContextService);

  if (!auth.isLoggedIn()) return router.createUrlTree(['/auth/login']);
  if (auth.isGestionnaire()) return true;

  const tontineId = ctx.tontineCouranteId();
  if (!tontineId) return router.createUrlTree(['/mes-tontines']);

  return membres.getMonProfil(tontineId).pipe(
    map(m => m.fonction === 'TRESORIER' ? true : router.createUrlTree(['/dashboard'])),
    catchError(() => of(router.createUrlTree(['/dashboard']))),
  );
};
