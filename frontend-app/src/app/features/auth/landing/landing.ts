import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { I18nService } from '../../../core/services/i18n.service';
import { ThemeService } from '../../../core/services/theme.service';

type Role = 'client' | 'operator' | 'admin';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [],
  templateUrl: './landing.html',
  styleUrl: './landing.scss',
})
export class Landing {
  private router = inject(Router);
  readonly i18n  = inject(I18nService);
  readonly theme = inject(ThemeService);

  selected = signal<Role | null>(null);

  roles: { id: Role; color: string; route: string }[] = [
    { id: 'client',   color: 'cyan',   route: '/auth/client'   },
    { id: 'operator', color: 'amber',  route: '/auth/operator' },
    { id: 'admin',    color: 'violet', route: '/auth/admin'    },
  ];

  select(role: Role): void {
    this.selected.set(role);
    const r = this.roles.find(x => x.id === role)!;
    setTimeout(() => this.router.navigate([r.route]), 160);
  }

  t(key: Parameters<I18nService['t']>[0]) { return this.i18n.t(key); }
}
