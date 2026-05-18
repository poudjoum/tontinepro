import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();

  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && auth.isLoggedIn()) {
        const refreshToken = auth.getRefreshToken();
        if (refreshToken) {
          return auth.refresh(refreshToken).pipe(
            switchMap(newAuth => {
              const retried = req.clone({
                setHeaders: { Authorization: `Bearer ${newAuth.accessToken}` }
              });
              return next(retried);
            }),
            catchError(() => {
              auth.logout();
              return throwError(() => err);
            })
          );
        }
        auth.logout();
      }
      return throwError(() => err);
    })
  );
};
