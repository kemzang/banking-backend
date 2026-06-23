import { AfterViewInit, Component, OnDestroy, inject, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { MfaModalComponent } from '../mfa-modal/mfa-modal';
import { I18nService } from '../../../core/services/i18n.service';
import { ThemeService } from '../../../core/services/theme.service';
import { environment } from '../../../../environments/environment';

declare const google: any;

type RoleCtx = 'client' | 'operator' | 'admin';

const ROLE_META: Record<RoleCtx, { badge: string; color: string; label: { fr: string; en: string } }> = {
  client:   { badge: 'role-badge--cyan',   color: '#00D4FF', label: { fr: 'Client',                   en: 'Customer'              } },
  operator: { badge: 'role-badge--amber',  color: '#F59E0B', label: { fr: 'Établissement financier',   en: 'Financial Institution' } },
  admin:    { badge: 'role-badge--violet', color: '#8B5CF6', label: { fr: 'Administrateur',            en: 'Administrator'         } },
};

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink, MfaModalComponent],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements AfterViewInit, OnDestroy {
  private auth   = inject(AuthService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);
  private toast  = inject(ToastService);
  readonly i18n  = inject(I18nService);
  readonly theme = inject(ThemeService);

  email      = '';
  motDePasse = '';
  showPwd    = signal(false);
  chargement = signal(false);
  erreur     = signal<string | null>(null);

  rateBlocked   = signal(false);
  rateCountdown = signal(0);
  private rateTimer?: ReturnType<typeof setInterval>;

  mfaVisible = signal(false);
  mfaEmail   = signal('');

  readonly googleActif = !!environment.googleClientId;

  // Contexte de rôle depuis l'URL (/auth/client | /auth/operator | /auth/admin)
  readonly roleCtx = computed<RoleCtx>(() => {
    const url = this.router.url;
    if (url.includes('operator')) return 'operator';
    if (url.includes('admin'))    return 'admin';
    return 'client';
  });

  readonly roleMeta = computed(() => ROLE_META[this.roleCtx()]);

  t(key: Parameters<I18nService['t']>[0]) { return this.i18n.t(key); }

  ngAfterViewInit(): void {
    // Banner "session expirée" ou "compte créé"
    const q = this.route.snapshot.queryParamMap;
    if (q.get('expired')) this.toast.warning('Votre session a expiré, reconnectez-vous.');
    if (q.get('registered')) this.toast.success('Compte créé — connectez-vous.');
    if (q.get('pending')) this.toast.success('Votre compte est en attente de validation par l’opérateur.');

    // Google Identity Services
    if (!this.googleActif || typeof google === 'undefined') return;
    try {
      google.accounts.id.initialize({
        client_id: environment.googleClientId,
        callback: (r: any) => this.onGoogle(r),
      });
      const el = document.getElementById('googleBtn');
      if (el) google.accounts.id.renderButton(el, { theme: 'outline', size: 'large', width: 320 });
    } catch { /* Google unavailable */ }
  }

  ngOnDestroy(): void {
    if (this.rateTimer) clearInterval(this.rateTimer);
  }

  seConnecter(): void {
    if (this.chargement() || this.rateBlocked()) return;
    this.erreur.set(null);
    this.chargement.set(true);

    this.auth.login({ email: this.email, motDePasse: this.motDePasse }).subscribe({
      next: () => this.postLogin(),
      error: (e) => this.handleLoginError(e),
    });
  }

  private postLogin(): void {
    this.chargement.set(false);
    if (this.auth.mfaEnabled()) {
      this.mfaEmail.set(this.auth.email());
      this.mfaVisible.set(true);
    } else {
      this.router.navigate([this.auth.redirectByRole()]);
    }
  }

  private handleLoginError(e: any): void {
    this.chargement.set(false);
    if (e.status === 401) {
      this.erreur.set('Email ou mot de passe incorrect.');
    } else if (e.status === 423) {
      this.erreur.set('Compte suspendu. Contactez l\'administrateur.');
    } else if (e.status === 429) {
      const retry = parseInt(e.headers?.get('Retry-After') ?? '60', 10);
      this.startRateLimit(retry);
    } else {
      this.toast.error('Erreur serveur, veuillez réessayer.');
    }
  }

  private startRateLimit(seconds: number): void {
    this.rateBlocked.set(true);
    this.rateCountdown.set(seconds);
    this.rateTimer = setInterval(() => {
      const n = this.rateCountdown() - 1;
      if (n <= 0) {
        this.rateBlocked.set(false);
        this.rateCountdown.set(0);
        clearInterval(this.rateTimer);
      } else {
        this.rateCountdown.set(n);
      }
    }, 1000);
  }

  private onGoogle(resp: any): void {
    this.chargement.set(true);
    this.auth.googleLogin(resp.credential).subscribe({
      next: () => { this.chargement.set(false); this.postLogin(); },
      error: () => { this.chargement.set(false); this.erreur.set('Connexion Google refusée.'); },
    });
  }

  onMfaSuccess(): void {
    this.mfaVisible.set(false);
    this.router.navigate([this.auth.redirectByRole()]);
  }

  onMfaClose(): void {
    // MFA obligatoire — on déconnecte si l'utilisateur ferme sans valider
    this.auth.logout();
  }
}
