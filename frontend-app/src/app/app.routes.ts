import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/guards/auth.guard';
import { Layout } from './layout/layout';

export const routes: Routes = [
  // ── Landing / Role selection ──────────────────────────
  { path: 'auth',
    loadComponent: () => import('./features/auth/landing/landing').then(m => m.Landing) },
  { path: 'auth/client',
    loadComponent: () => import('./features/auth/login-client/login-client').then(m => m.LoginClient) },
  { path: 'auth/operator',
    loadComponent: () => import('./features/auth/login-operator/login-operator').then(m => m.LoginOperator) },
  { path: 'auth/admin',
    loadComponent: () => import('./features/auth/login-admin/login-admin').then(m => m.LoginAdmin) },

  // ── Additional auth pages ─────────────────────────────
  { path: 'register',
    loadComponent: () => import('./features/auth/register/register').then(m => m.Register) },
  { path: 'auth/mfa',
    loadComponent: () => import('./features/auth/mfa-verify/mfa-verify').then(m => m.MfaVerify) },
  { path: 'auth/forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password').then(m => m.ForgotPassword) },
  { path: 'auth/reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password').then(m => m.ResetPassword) },
  { path: 'settings/mfa-setup',
    loadComponent: () => import('./features/settings/mfa-setup/mfa-setup').then(m => m.MfaSetup) },

  // ── Compat aliases ────────────────────────────────────
  { path: 'login',    redirectTo: '/auth/client', pathMatch: 'full' },
  { path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password').then(m => m.ForgotPassword) },
  { path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password').then(m => m.ResetPassword) },

  // Espaces cloisonnes par role.
  {
    path: 'admin', component: Layout, canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN_PLATFORM'] },
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard) },
      { path: 'operators', loadComponent: () => import('./features/operateurs/operateurs').then(m => m.Operateurs) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  {
    path: 'operator', component: Layout, canActivate: [authGuard, roleGuard],
    data: { roles: ['OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/operator/dashboard/operator-dashboard').then(m => m.OperatorDashboard) },
      { path: 'clients', loadComponent: () => import('./features/clients/clients').then(m => m.Clients) },
      { path: 'validations', data: { pendingOnly: true }, loadComponent: () => import('./features/clients/clients').then(m => m.Clients) },
      { path: 'accounts', loadComponent: () => import('./features/comptes/comptes').then(m => m.Comptes) },
      { path: 'accounts/:id', loadComponent: () => import('./features/comptes/compte-detail').then(m => m.CompteDetail) },
      { path: 'transactions', loadComponent: () => import('./features/transactions/transactions').then(m => m.Transactions) },
      { path: 'loans', loadComponent: () => import('./features/prets/prets').then(m => m.Prets) },
      { path: 'agents', canActivate: [roleGuard], data: { roles: ['OPERATOR_ADMIN'] },
        loadComponent: () => import('./features/operator/agents/agents').then(m => m.OperatorAgents) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  {
    path: 'client', component: Layout, canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENT'] },
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/client/dashboard/client-dashboard').then(m => m.ClientDashboard) },
      { path: 'profile', loadComponent: () => import('./features/client/profile/client-profile').then(m => m.ClientProfile) },
      { path: 'accounts', loadComponent: () => import('./features/comptes/comptes').then(m => m.Comptes) },
      { path: 'accounts/:id', loadComponent: () => import('./features/comptes/compte-detail').then(m => m.CompteDetail) },
      { path: 'transactions', loadComponent: () => import('./features/transactions/transactions').then(m => m.Transactions) },
      { path: 'loans', loadComponent: () => import('./features/client/loans/client-loans').then(m => m.ClientLoans) },
      { path: 'documents', loadComponent: () => import('./features/documents/documents').then(m => m.Documents) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notifications').then(m => m.Notifications) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  // ── Zone protégée historique ─────────────────────────
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard) },
      { path: 'clients',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/clients/clients').then(m => m.Clients) },
      { path: 'comptes',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/comptes/comptes').then(m => m.Comptes) },
      { path: 'comptes/:id',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/comptes/compte-detail').then(m => m.CompteDetail) },
      { path: 'transactions',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/transactions/transactions').then(m => m.Transactions) },
      { path: 'prets',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/prets/prets').then(m => m.Prets) },
      { path: 'notifications',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/notifications/notifications').then(m => m.Notifications) },
      { path: 'documents',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/documents/documents').then(m => m.Documents) },
      { path: 'statistiques',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT'] },
        loadComponent: () => import('./features/statistiques/statistiques').then(m => m.Statistiques) },
      { path: 'operateurs',
        canActivate: [roleGuard], data: { roles: ['ADMIN_PLATFORM'] },
        loadComponent: () => import('./features/operateurs/operateurs').then(m => m.Operateurs) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },

  { path: '**', redirectTo: '/auth' },
];
