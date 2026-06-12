import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
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
