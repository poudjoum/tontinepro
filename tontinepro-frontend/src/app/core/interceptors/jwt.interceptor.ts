import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** Requêtes dont un 401 est définitif : les rejouer ne ferait que boucler. */
const SANS_REJEU = ['/auth/login', '/auth/refresh', '/auth/register'];

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      const rejouable = err.status === 401
        && auth.isLoggedIn()
        && !SANS_REJEU.some(chemin => req.url.includes(chemin));

      if (!rejouable) return throwError(() => err);

      if (!auth.getRefreshToken()) {
        auth.logout();
        return throwError(() => err);
      }

      // rafraichir() partage un unique appel : plusieurs requêtes tombant en
      // 401 ensemble attendent le même jeton au lieu d'en demander chacune un,
      // ce qui révoquait le refresh token sous les pieds des autres.
      return auth.rafraichir().pipe(
        // Placé avant switchMap : seul l'échec du rafraîchissement déconnecte.
        // Une erreur de la requête rejouée (500, 404…) doit remonter telle
        // quelle, sans faire perdre sa session à l'utilisateur.
        catchError(() => {
          auth.logout();
          return throwError(() => err);
        }),
        switchMap(nouvelAuth => next(req.clone({
          setHeaders: { Authorization: `Bearer ${nouvelAuth.accessToken}` }
        })))
      );
    })
  );
};
