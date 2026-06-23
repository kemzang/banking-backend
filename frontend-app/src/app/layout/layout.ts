import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { I18nService } from '../core/services/i18n.service';
import { ThemeService } from '../core/services/theme.service';
import { UserResponse } from '../core/models/auth.models';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private auth    = inject(AuthService);
  private router  = inject(Router);
  readonly i18n   = inject(I18nService);
  readonly theme  = inject(ThemeService);

  user = signal<UserResponse | null>(null);

  constructor() {
    this.auth.me().subscribe({ next: (u) => this.user.set(u) });
  }

  estAdmin(): boolean     { return this.auth.hasRole('ADMIN_PLATFORM'); }
  estOperateur(): boolean {
    return this.auth.hasRole('OPERATOR_ADMIN', 'OPERATOR_AGENT');
  }
  estOperatorAdmin(): boolean { return this.auth.hasRole('OPERATOR_ADMIN'); }
  estClient(): boolean { return this.auth.hasRole('CLIENT'); }

  route(segment: string): string {
    if (this.estAdmin()) return `/admin/${segment}`;
    if (this.estOperateur()) return `/operator/${segment}`;
    return `/client/${segment}`;
  }

  deconnexion(): void {
    this.auth.logout();
    this.router.navigate(['/auth']);
  }

  t(key: Parameters<I18nService['t']>[0]) { return this.i18n.t(key); }
}
