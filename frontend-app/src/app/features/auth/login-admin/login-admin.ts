import { Component, inject } from '@angular/core';
import { I18nService } from '../../../core/services/i18n.service';
import { AuthLayoutComponent } from '../../../shared/auth-layout/auth-layout';
import { AuthFormComponent } from '../../../shared/auth-layout/auth-form';

@Component({
  selector: 'app-login-admin',
  standalone: true,
  imports: [AuthLayoutComponent, AuthFormComponent],
  templateUrl: './login-admin.html',
  styleUrl: '../auth-pages.scss',
})
export class LoginAdmin {
  readonly i18n = inject(I18nService);
  t(k: Parameters<I18nService['t']>[0]) { return this.i18n.t(k); }
}
