import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { I18nService } from '../../../core/services/i18n.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss',
})
export class ResetPassword implements OnInit {
  private auth   = inject(AuthService);
  private router = inject(Router);
  private route  = inject(ActivatedRoute);
  private toast  = inject(ToastService);
  readonly i18n  = inject(I18nService);
  readonly theme = inject(ThemeService);

  token = signal('');
  password = signal('');
  confirmPassword = signal('');
  showPassword = signal(false);
  showConfirmPassword = signal(false);
  
  loading = signal(false);
  success = signal(false);
  error = signal('');
  
  // Password validation errors
  passwordErrors = signal<string[]>([]);
  confirmError = signal('');

  // Password strength calculation
  passwordStrength = computed(() => {
    const pwd = this.password();
    if (!pwd) return { score: 0, label: '', color: '' };
    
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    
    const labels = ['', this.t('password_weak'), this.t('password_fair'), this.t('password_strong'), this.t('password_excellent')];
    const colors = ['', 'bg-red-500', 'bg-amber-500', 'bg-lime-500', 'bg-cyan-500'];
    
    return { score, label: labels[score], color: colors[score] };
  });

  // Password requirements checklist
  passwordRequirements = computed(() => {
    const pwd = this.password();
    return [
      { key: 'pwd_req_length', met: pwd.length >= 8 },
      { key: 'pwd_req_upper', met: /[A-Z]/.test(pwd) },
      { key: 'pwd_req_number', met: /[0-9]/.test(pwd) },
      { key: 'pwd_req_special', met: /[^A-Za-z0-9]/.test(pwd) }
    ];
  });

  t(key: Parameters<I18nService['t']>[0], vars?: Record<string, string>) { 
    return this.i18n.t(key, vars); 
  }

  ngOnInit(): void {
    // Get token from query params
    const tokenParam = this.route.snapshot.queryParams['token'];
    if (!tokenParam) {
      this.router.navigate(['/auth/forgot-password']);
      return;
    }
    
    this.token.set(tokenParam);
  }

  onPasswordChange(value: string): void {
    this.password.set(value);
    this.validatePassword();
    this.validateConfirmPassword();
  }

  onConfirmPasswordChange(value: string): void {
    this.confirmPassword.set(value);
    this.validateConfirmPassword();
  }

  validatePassword(): void {
    const pwd = this.password();
    const errors: string[] = [];
    
    if (pwd && this.passwordStrength().score < 3) {
      errors.push('Mot de passe trop faible');
    }
    
    this.passwordErrors.set(errors);
  }

  validateConfirmPassword(): void {
    const pwd = this.password();
    const confirm = this.confirmPassword();
    
    if (confirm && pwd !== confirm) {
      this.confirmError.set(this.t('passwords_no_match'));
    } else {
      this.confirmError.set('');
    }
  }

  canSubmit(): boolean {
    return (
      this.password().length > 0 &&
      this.confirmPassword().length > 0 &&
      this.passwordStrength().score >= 3 &&
      this.password() === this.confirmPassword() &&
      !this.loading()
    );
  }

  onSubmit(): void {
    if (!this.canSubmit()) return;
    
    this.loading.set(true);
    this.error.set('');
    
    this.auth.resetPassword({
      token: this.token(),
      newPassword: this.password()
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        // Auto-redirect after 3 seconds
        setTimeout(() => {
          this.router.navigate(['/auth/client'], { 
            queryParams: { reset: 'true' } 
          });
        }, 3000);
      },
      error: (error) => {
        this.loading.set(false);
        if (error.status === 400 || error.status === 410) {
          this.error.set(this.t('reset_expired'));
        } else {
          this.error.set('Erreur lors de la réinitialisation');
        }
      }
    });
  }
}