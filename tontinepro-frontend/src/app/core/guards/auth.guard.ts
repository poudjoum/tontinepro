import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
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

  const tontineId = ctx.tontineCouranteId();
  if (!tontineId) return router.createUrlTree(['/mes-tontines']);

  // Même règle que sec.peutEncaisser : trésorier ou président toujours,
  // secrétaire seulement faute de trésorier désigné. Un simple isGestionnaire()
  // ne suffisait pas — il laissait entrer tout secrétaire, y compris dans une
  // tontine pourvue d'un trésorier, sur une matrice dont chaque clic finissait
  // en 403.
  return membres.getMonProfil(tontineId).pipe(
    switchMap(moi => {
      if (moi.fonction === 'TRESORIER' || moi.fonction === 'PRESIDENT') return of(true);
      if (moi.fonction !== 'SECRETAIRE') return of(router.createUrlTree(['/dashboard']));
      // La liste n'est demandée que dans ce cas : un trésorier, en rôle MEMBRE,
      // n'a pas le droit de l'appeler et n'en a pas l'usage.
      return membres.getAll(tontineId, 'ACTIF').pipe(
        map(tous => tous.some(m => m.fonction === 'TRESORIER')
          ? router.createUrlTree(['/dashboard'])
          : true));
    }),
    catchError(() => of(router.createUrlTree(['/dashboard']))),
  );
};
