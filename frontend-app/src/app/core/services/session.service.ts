import { Injectable, NgZone, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { ToastService } from './toast.service';

const INACTIVITY_WARN_MS  = 28 * 60 * 1000;  // 28 min → affiche modale
const INACTIVITY_LIMIT_MS = 30 * 60 * 1000;  // 30 min → déconnexion

@Injectable({ providedIn: 'root' })
export class SessionService {
  private auth  = inject(AuthService);
  private toast = inject(ToastService);
  private zone  = inject(NgZone);

  readonly showWarning  = signal(false);
  readonly countdown    = signal(120);  // 2 min en secondes

  private warnTimer?:    ReturnType<typeof setTimeout>;
  private logoutTimer?:  ReturnType<typeof setTimeout>;
  private countInterval?: ReturnType<typeof setInterval>;
  private lastActivity   = Date.now();

  /** Démarre le tracking — appelé dans AppComponent */
  start(): void {
    const events = ['mousemove', 'keydown', 'click', 'touchstart'];
    events.forEach(ev =>
      window.addEventListener(ev, () => this.onActivity(), { passive: true })
    );

    // Multi-onglets : si token supprimé ailleurs, déconnecter ici
    window.addEventListener('storage', (e) => {
      if (e.key === 'bank_token' && !e.newValue) this.auth.logout();
    });

    this.resetTimers();
  }

  private onActivity(): void {
    this.lastActivity = Date.now();
    if (this.showWarning()) return; // ne pas reset si modale déjà visible
    this.resetTimers();
  }

  private resetTimers(): void {
    clearTimeout(this.warnTimer);
    clearTimeout(this.logoutTimer);
    clearInterval(this.countInterval);
    this.showWarning.set(false);

    this.zone.runOutsideAngular(() => {
      this.warnTimer = setTimeout(() => {
        this.zone.run(() => this.triggerWarning());
      }, INACTIVITY_WARN_MS);
    });
  }

  private triggerWarning(): void {
    this.showWarning.set(true);
    this.countdown.set(120);

    this.countInterval = setInterval(() => {
      const n = this.countdown() - 1;
      if (n <= 0) {
        clearInterval(this.countInterval);
        this.auth.logout();
      } else {
        this.countdown.set(n);
      }
    }, 1000);
  }

  extend(): void {
    this.auth.refresh().subscribe({
      next: () => { this.resetTimers(); this.toast.success('Session prolongée.'); },
      error: () => this.auth.logout(),
    });
  }

  logoutNow(): void { this.auth.logout(); }
}
