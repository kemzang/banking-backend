import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { Layout } from './layout/layout';

export const routes: Routes = [
  // Pages publiques (hors layout)
  { path: 'login', loadComponent: () => import('./features/auth/login/login').then((m) => m.Login) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register').then((m) => m.Register) },

  // Zone protegee : layout avec menu + pages enfants
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard) },
      { path: 'clients', loadComponent: () => import('./features/clients/clients').then((m) => m.Clients) },
      { path: 'operateurs', loadComponent: () => import('./features/operateurs/operateurs').then((m) => m.Operateurs) },
      { path: 'documents', loadComponent: () => import('./features/documents/documents').then((m) => m.Documents) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
