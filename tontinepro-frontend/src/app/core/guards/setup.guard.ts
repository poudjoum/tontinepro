import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { SetupService } from '../services/setup.service';

/** Autorise l'accès à /setup uniquement si la plateforme n'est PAS encore configurée. */
export const notConfiguredGuard: CanActivateFn = () => {
  const setup = inject(SetupService);
  const router = inject(Router);
  return setup.isConfigured().pipe(
    map(configured => configured ? router.createUrlTree(['/auth/login']) : true)
  );
};

/** Redirige vers /setup si la plateforme n'est pas encore configurée. */
export const configuredGuard: CanActivateFn = () => {
  const setup = inject(SetupService);
  const router = inject(Router);
  return setup.isConfigured().pipe(
    map(configured => configured ? true : router.createUrlTree(['/setup']))
  );
};
