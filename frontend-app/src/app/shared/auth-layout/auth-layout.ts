import { Component, Input, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../core/services/i18n.service';
import { ThemeService } from '../../core/services/theme.service';

export type AuthAccent = 'cyan' | 'amber' | 'violet';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.scss',
})
export class AuthLayoutComponent {
  @Input() accent: AuthAccent = 'cyan';

  readonly i18n  = inject(I18nService);
  readonly theme = inject(ThemeService);

  t(key: Parameters<I18nService['t']>[0]) { return this.i18n.t(key); }
}
