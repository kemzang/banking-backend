import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/** Vérifie connexion + expiry JWT à chaque navigation */
export const authGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (!auth.hasValidToken()) {
    if (auth.token) auth.logout();
    return router.parseUrl('/auth?expired=1');
  }
  return true;
};

/** Vérifie le rôle requis (route.data.roles) */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  const requis = (route.data?.['roles'] as string[]) ?? [];

  if (requis.length === 0 || auth.hasRole(...requis)) return true;
  return router.parseUrl('/dashboard');
};
