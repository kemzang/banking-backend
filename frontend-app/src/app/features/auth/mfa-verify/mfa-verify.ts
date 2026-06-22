import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { I18nService } from '../../../core/services/i18n.service';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-mfa-verify',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './mfa-verify.html',
  styleUrl: './mfa-verify.scss',
})
export class MfaVerify implements OnInit {
  private auth  = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private toast = inject(ToastService);
  readonly i18n = inject(I18nService);
  readonly theme = inject(ThemeService);

  email = signal('');
  loading = signal(false);
  useBackupCode = signal(false);
  attempts = signal(0);
  maxAttempts = 5;
  
  // TOTP code (6 digits)
  digits = signal(['', '', '', '', '', '']);
  
  // Backup code (8 chars)
  backupCode = signal('');
  
  // Timer for resend
  resendTimer = signal(30);
  canResend = computed(() => this.resendTimer() === 0);
  
  private timerInterval?: ReturnType<typeof setInterval>;
  
  // Error states
  error = signal('');
  shakeAnimation = signal(false);

  t(key: Parameters<I18nService['t']>[0], vars?: Record<string, string>) { 
    return this.i18n.t(key, vars); 
  }

  ngOnInit(): void {
    // Check if user has valid token but needs MFA
    if (!this.auth.hasValidToken() || !this.auth.mfaEnabled()) {
      this.router.navigate(['/auth']);
      return;
    }
    
    this.email.set(this.auth.email());
    this.startResendTimer();
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  startResendTimer(): void {
    this.resendTimer.set(30);
    this.timerInterval = setInterval(() => {
      const current = this.resendTimer();
      if (current > 0) {
        this.resendTimer.set(current - 1);
      } else {
        clearInterval(this.timerInterval);
      }
    }, 1000);
  }

  onDigitInput(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, ''); // Only digits
    
    if (value.length > 1) {
      value = value.slice(0, 1);
    }
    
    // Update the digits array
    const newDigits = [...this.digits()];
    newDigits[index] = value;
    this.digits.set(newDigits);
    
    // Auto-focus next input
    if (value && index < 5) {
      const nextInput = document.querySelector(`#digit-${index + 1}`) as HTMLInputElement;
      nextInput?.focus();
    }
    
    // Auto-submit when all 6 digits are filled
    if (newDigits.every(d => d) && newDigits.length === 6) {
      setTimeout(() => this.verifyTotp(), 100);
    }
  }

  onDigitKeyDown(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace') {
      const currentDigits = this.digits();
      if (!currentDigits[index] && index > 0) {
        // If current field is empty, go back to previous and clear it
        const newDigits = [...currentDigits];
        newDigits[index - 1] = '';
        this.digits.set(newDigits);
        
        const prevInput = document.querySelector(`#digit-${index - 1}`) as HTMLInputElement;
        prevInput?.focus();
      }
    }
  }

  onDigitPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const paste = event.clipboardData?.getData('text') || '';
    const digits = paste.replace(/\D/g, '').slice(0, 6).split('');
    
    // Pad with empty strings if needed
    while (digits.length < 6) {
      digits.push('');
    }
    
    this.digits.set(digits);
    
    // Focus the first empty field or the last field
    const firstEmpty = digits.findIndex(d => !d);
    const focusIndex = firstEmpty !== -1 ? firstEmpty : 5;
    const input = document.querySelector(`#digit-${focusIndex}`) as HTMLInputElement;
    input?.focus();
  }

  verifyTotp(): void {
    if (this.loading()) return;
    
    const code = this.digits().join('');
    if (code.length !== 6) return;
    
    this.clearError();
    this.loading.set(true);
    
    this.auth.mfaVerify({ 
      email: this.email(), 
      code 
    }).subscribe({
      next: () => {
        this.loading.set(false);
        // Redirect based on role
        this.router.navigate([this.auth.redirectByRole()]);
      },
      error: (error) => {
        this.loading.set(false);
        this.handleError(error);
        this.clearDigits();
        this.shake();
      }
    });
  }

  verifyBackupCode(): void {
    if (this.loading() || !this.backupCode().trim()) return;
    
    this.clearError();
    this.loading.set(true);
    
    this.auth.mfaVerifyBackup({ 
      email: this.email(), 
      backupCode: this.backupCode().toUpperCase() 
    }).subscribe({
      next: (response: any) => {
        this.loading.set(false);
        if (response.backupCodesRemaining !== undefined) {
          this.toast.success(
            this.i18n.lang() === 'fr' 
              ? `Code de secours utilisé — ${response.backupCodesRemaining} codes restants`
              : `Backup code used — ${response.backupCodesRemaining} remaining`
          );
        }
        this.router.navigate([this.auth.redirectByRole()]);
      },
      error: (error) => {
        this.loading.set(false);
        this.handleError(error);
        this.backupCode.set('');
        this.shake();
      }
    });
  }

  resendCode(): void {
    if (!this.canResend() || this.loading()) return;
    
    this.loading.set(true);
    
    this.auth.mfaResend({ email: this.email() }).subscribe({
      next: () => {
        this.loading.set(false);
        this.startResendTimer();
        this.toast.success(
          this.i18n.lang() === 'fr' ? 'Code renvoyé' : 'Code resent'
        );
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Erreur lors de l\'envoi du code');
      }
    });
  }

  toggleBackupMode(): void {
    this.useBackupCode.set(!this.useBackupCode());
    this.clearError();
    this.clearDigits();
    this.backupCode.set('');
  }

  backToLogin(): void {
    this.auth.logout();
    this.router.navigate(['/auth']);
  }

  private handleError(error: any): void {
    this.attempts.update(current => current + 1);
    
    if (error.status === 401) {
      this.error.set(this.t('mfa_wrong'));
      if (this.attempts() < this.maxAttempts) {
        const attemptsText = this.t('mfa_attempts', { n: this.attempts().toString() });
        this.error.update(current => current + ` (${attemptsText})`);
      }
    } else if (error.status === 423) {
      this.error.set(this.t('mfa_locked'));
    } else {
      this.error.set('Erreur de vérification');
    }
    
    // Lock after max attempts
    if (this.attempts() >= this.maxAttempts) {
      this.error.set(this.t('mfa_locked'));
    }
  }

  private clearError(): void {
    this.error.set('');
  }

  private clearDigits(): void {
    this.digits.set(['', '', '', '', '', '']);
    // Focus first input
    setTimeout(() => {
      const firstInput = document.querySelector('#digit-0') as HTMLInputElement;
      firstInput?.focus();
    }, 50);
  }

  private shake(): void {
    this.shakeAnimation.set(true);
    setTimeout(() => this.shakeAnimation.set(false), 400);
  }
}