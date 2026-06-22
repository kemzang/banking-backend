import { HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  const token  = auth.token;

  // Clone avec Bearer token + header CSRF sur les mutations
  let cloned = req;
  if (token) {
    cloned = cloned.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) {
    cloned = cloned.clone({ setHeaders: { 'X-Requested-With': 'XMLHttpRequest' } });
  }

  return next(cloned).pipe(
    catchError(err => {
      // Token expiré ou invalide → déconnexion silencieuse
      if (err.status === HttpStatusCode.Unauthorized) {
        // Ne pas boucler sur les routes d'auth elles-mêmes
        if (!req.url.includes('/api/auth/')) {
          auth.logout();
          router.navigate(['/login'], { queryParams: { expired: '1' } });
        }
      }
      return throwError(() => err);
    }),
  );
};
