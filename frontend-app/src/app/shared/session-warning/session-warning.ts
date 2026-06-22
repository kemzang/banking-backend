import { Component, inject } from '@angular/core';
import { SessionService } from '../../core/services/session.service';

@Component({
  selector: 'app-session-warning',
  standalone: true,
  template: `
    @if (session.showWarning()) {
      <div class="sw-overlay" role="dialog" aria-modal="true" aria-labelledby="sw-title">
        <div class="sw-card">
          <div class="sw-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="#F59E0B" stroke-width="1.8"/>
              <path d="M12 6v6l4 2" stroke="#F59E0B" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <h2 id="sw-title">Session sur le point d'expirer</h2>
          <p>
            Votre session expire dans
            <strong class="sw-count">{{ session.countdown() }}s</strong>
          </p>
          <div class="sw-actions">
            <button class="sw-btn sw-btn--primary" (click)="session.extend()">
              Rester connecté
            </button>
            <button class="sw-btn sw-btn--ghost" (click)="session.logoutNow()">
              Se déconnecter
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styleUrl: './session-warning.scss',
})
export class SessionWarningComponent {
  session = inject(SessionService);
}
