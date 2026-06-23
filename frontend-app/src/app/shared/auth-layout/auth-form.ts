import {
  AfterViewInit, Component, EventEmitter, Input,
  OnDestroy, Output, inject, signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { I18nService } from '../../core/services/i18n.service';
import { MfaModalComponent } from '../../features/auth/mfa-modal/mfa-modal';
import { environment } from '../../../environments/environment';

export type PortalRole = 'client' | 'operator' | 'admin';
export type AuthTab    = 'pwd' | 'google';

declare const google: any;

// Maps which JWT role is expected per portal
const EXPECTED: Record<PortalRole, string[]> = {
  client:   ['CLIENT'],
  operator: ['OPERATOR_ADMIN', 'OPERATOR_AGENT'],
  admin:    ['ADMIN_PLATFORM'],
};

const WRONG_ROLE_KEY: Record<PortalRole, string> = {
  client:   'login_err_wrong_role_client',
  operator: 'login_err_wrong_role_operator',
  admin:    'login_err_wrong_role_admin',
};

const HINT_KEY: Record<string, string> = {
  ADMIN_PLATFORM: 'login_hint_use_admin',
  OPERATOR_ADMIN: 'login_hint_use_operator',
  OPERATOR_AGENT: 'login_hint_use_operator',
  CLIENT:         'login_hint_use_client',
};

const HINT_ROUTE: Record<string, string> = {
  ADMIN_PLATFORM: '/auth/admin',
  OPERATOR_ADMIN: '/auth/operator',
  OPERATOR_AGENT: '/auth/operator',
  CLIENT:         '/auth/client',
};

const DASHBOARD: Record<string, string> = {
  ADMIN_PLATFORM: '/admin/dashboard',
  OPERATOR_ADMIN: '/operator/dashboard',
  OPERATOR_AGENT: '/operator/dashboard',
  CLIENT:         '/client/dashboard',
};

const LOGIN_TYPE: Record<PortalRole, 'CLIENT_LOGIN' | 'ADMIN_LOGIN' | 'OPERATOR_LOGIN'> = {
  client: 'CLIENT_LOGIN',
  admin: 'ADMIN_LOGIN',
  operator: 'OPERATOR_LOGIN',
};

@Component({
  selector: 'app-auth-form',
  standalone: true,
  imports: [FormsModule, RouterLink, MfaModalComponent],
  templateUrl: './auth-form.html',
  styleUrl: './auth-form.scss',
})
export class AuthFormComponent implements AfterViewInit, OnDestroy {
  @Input() portal: PortalRole = 'client';
  @Input() showTabs    = true;
  @Input() showForgot  = true;
  @Input() showRegister = false;
  @Input() showOpCode  = false;
  @Input() accent      = 'cyan';
  @Output() loginOk = new EventEmitter<void>();

  private auth   = inject(AuthService);
  private router = inject(Router);
  private toast  = inject(ToastService);
  readonly i18n  = inject(I18nService);

  email      = '';
  motDePasse = '';
  opCode     = '';
  rememberMe = false;
  showPwd    = signal(false);
  tab        = signal<AuthTab>('pwd');
  chargement = signal(false);
  erreur     = signal<string | null>(null);
  hintRoute  = signal<string | null>(null);
  hintLabel  = signal<string | null>(null);

  rateBlocked   = signal(false);
  rateCountdown = signal(0);
  private rateTimer?: ReturnType<typeof setInterval>;

  mfaVisible = signal(false);
  mfaEmail   = signal('');

  readonly googleActif = !!environment.googleClientId;

  t(key: Parameters<I18nService['t']>[0]) { return this.i18n.t(key); }

  ngAfterViewInit(): void {
    if (!this.googleActif || typeof google === 'undefined') return;
    try {
      google.accounts.id.initialize({
        client_id: environment.googleClientId,
        callback: (r: any) => this.onGoogle(r),
      });
      const el = document.getElementById('googleBtnSlot');
      if (el) google.accounts.id.renderButton(el, { theme: 'outline', size: 'large', width: 340 });
    } catch { /* Google unavailable */ }
  }

  ngOnDestroy(): void {
    if (this.rateTimer) clearInterval(this.rateTimer);
  }

  submit(): void {
    if (this.chargement() || this.rateBlocked()) return;
    this.resetErrors();
    this.chargement.set(true);

    this.auth.login({
      email: this.email,
      motDePasse: this.motDePasse,
      loginType: LOGIN_TYPE[this.portal],
    }).subscribe({
      next: () => this.onSuccess(),
      error: (e) => this.onError(e),
    });
  }

  private onSuccess(): void {
    this.chargement.set(false);
    // Role mismatch check
    const roles  = this.auth.roles();
    const expected = EXPECTED[this.portal];
    if (!expected.some(role => roles.includes(role))) {
      // Find what role they actually have
      const actual = ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT', 'CLIENT']
        .find(r => roles.includes(r));
      const errKey = WRONG_ROLE_KEY[this.portal] as Parameters<I18nService['t']>[0];
      this.erreur.set(this.t(errKey));
      if (actual) {
        this.hintRoute.set(HINT_ROUTE[actual]);
        this.hintLabel.set(this.t(HINT_KEY[actual] as Parameters<I18nService['t']>[0]));
      }
      // Clear stored token — wrong portal
      this.auth.discardToken();
      return;
    }

    if (this.auth.mfaEnabled()) {
      this.mfaEmail.set(this.auth.email());
      this.mfaVisible.set(true);
      return;
    }

    this.redirect();
  }

  private redirect(): void {
    const roles = this.auth.roles();
    const role = ['ADMIN_PLATFORM', 'OPERATOR_ADMIN', 'OPERATOR_AGENT', 'CLIENT']
      .find(r => roles.includes(r)) ?? 'CLIENT';
    this.router.navigate([DASHBOARD[role]]);
  }

  private onError(e: any): void {
    this.chargement.set(false);
    if (e.status === 401) {
      this.erreur.set(this.t('login_err_401'));
    } else if (e.status === 403) {
      const errKey = WRONG_ROLE_KEY[this.portal] as Parameters<I18nService['t']>[0];
      this.erreur.set(this.t(errKey));
    } else if (e.status === 423) {
      this.erreur.set(this.t('login_err_423'));
    } else if (e.status === 429) {
      const n = parseInt(e.headers?.get('Retry-After') ?? '60', 10);
      this.rateBlocked.set(true);
      this.rateCountdown.set(n);
      this.rateTimer = setInterval(() => {
        const r = this.rateCountdown() - 1;
        if (r <= 0) { this.rateBlocked.set(false); clearInterval(this.rateTimer); }
        else this.rateCountdown.set(r);
      }, 1000);
    } else {
      this.toast.error(this.t('login_err_server'));
    }
  }

  private onGoogle(resp: any): void {
    this.resetErrors();
    this.chargement.set(true);
    this.auth.googleLogin(resp.credential).subscribe({
      next: () => this.onSuccess(),
      error: () => { this.chargement.set(false); this.erreur.set(this.t('login_err_401')); },
    });
  }

  onMfaSuccess(): void {
    this.mfaVisible.set(false);
    this.redirect();
  }

  onMfaClose(): void { this.auth.logout(); }

  private resetErrors(): void {
    this.erreur.set(null);
    this.hintRoute.set(null);
    this.hintLabel.set(null);
  }
}
