import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';
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
      {
        path: 'clients',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'OPERATEUR'] },
        loadComponent: () => import('./features/clients/clients').then((m) => m.Clients),
      },
      { path: 'comptes', loadComponent: () => import('./features/comptes/comptes').then((m) => m.Comptes) },
      { path: 'transactions', loadComponent: () => import('./features/transactions/transactions').then((m) => m.Transactions) },
      { path: 'prets', loadComponent: () => import('./features/prets/prets').then((m) => m.Prets) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notifications').then((m) => m.Notifications) },
      { path: 'documents', canActivate: [roleGuard], data: { roles: ['ADMIN', 'OPERATEUR'] }, loadComponent: () => import('./features/documents/documents').then((m) => m.Documents) },
      {
        path: 'statistiques',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN', 'OPERATEUR'] },
        loadComponent: () => import('./features/statistiques/statistiques').then((m) => m.Statistiques),
      },
      {
        path: 'operateurs',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./features/operateurs/operateurs').then((m) => m.Operateurs),
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
