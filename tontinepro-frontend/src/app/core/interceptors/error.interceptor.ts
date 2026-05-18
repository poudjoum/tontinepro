import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // Extrait le message d'erreur du ProblemDetail (RFC 9457) ou du corps brut
      const message: string =
        err.error?.detail ??
        err.error?.message ??
        err.message ??
        'Une erreur est survenue';

      return throwError(() => ({ status: err.status, message }));
    })
  );
};
