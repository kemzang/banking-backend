import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Empeche d'acceder a une page protegee sans etre connecte.
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.connecte()) {
    return true;
  }
  router.navigate(['/login']);
  return false;
};

// Restreint l'acces selon les roles (route.data.roles = ['ADMIN', ...]).
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const requis = (route.data?.['roles'] as string[]) ?? [];
  if (requis.length === 0 || auth.hasRole(...requis)) {
    return true;
  }
  router.navigate(['/dashboard']);
  return false;
};
