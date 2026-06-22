import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { I18nService } from '../../../core/services/i18n.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss',
})
export class ForgotPassword {
  private auth  = inject(AuthService);
  private toast = inject(ToastService);
  readonly i18n = inject(I18nService);
  readonly theme = inject(ThemeService);

  email = signal('');
  loading = signal(false);
  sent = signal(false);
  error = signal('');

  t(key: Parameters<I18nService['t']>[0], vars?: Record<string, string>) { 
    return this.i18n.t(key, vars); 
  }

  validateEmail(): boolean {
    const emailValue = this.email().trim();
    
    if (!emailValue) {
      this.error.set(this.t('required'));
      return false;
    }
    
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailValue)) {
      this.error.set('Email invalide');
      return false;
    }
    
    this.error.set('');
    return true;
  }

  onSubmit(): void {
    if (this.loading() || this.sent() || !this.validateEmail()) return;
    
    this.loading.set(true);
    
    this.auth.forgotPassword({ email: this.email() }).subscribe({
      next: () => {
        this.loading.set(false);
        this.sent.set(true);
      },
      error: () => {
        this.loading.set(false);
        // Always show success state for security (don't reveal if email exists)
        this.sent.set(true);
      }
    });
  }

  onEmailChange(value: string): void {
    this.email.set(value);
    if (this.error()) {
      this.error.set('');
    }
  }
}