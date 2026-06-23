import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { I18nService } from '../../../core/services/i18n.service';
import { ThemeService } from '../../../core/services/theme.service';

interface MfaSetupResponse {
  secret: string;
  qrCodeUrl: string;
  backupCodes: string[];
}

@Component({
  selector: 'app-mfa-setup',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './mfa-setup.html',
  styleUrl: './mfa-setup.scss',
})
export class MfaSetup implements OnInit {
  private auth  = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);
  readonly i18n = inject(I18nService);
  readonly theme = inject(ThemeService);

  currentStep = signal(1);
  loading = signal(false);
  
  // Step 1 data
  setupData = signal<MfaSetupResponse | null>(null);
  showManualEntry = signal(false);
  
  // Step 2 - verification
  verificationCode = signal(['', '', '', '', '', '']);
  verificationError = signal('');
  
  // Step 3 - backup codes
  backupCodesConfirmed = signal(false);

  formattedSecret = computed(() => {
    const secret = this.setupData()?.secret;
    if (!secret) return '';
    return secret.match(/.{1,4}/g)?.join(' ') || secret;
  });

  t(key: Parameters<I18nService['t']>[0], vars?: Record<string, string>) { 
    return this.i18n.t(key, vars); 
  }

  ngOnInit(): void {
    // Ensure user is authenticated
    if (!this.auth.hasValidToken()) {
      this.router.navigate(['/auth']);
      return;
    }
    
    this.loadSetupData();
  }

  loadSetupData(): void {
    this.loading.set(true);
    
    this.auth.mfaSetup().subscribe({
      next: (data) => {
        this.loading.set(false);
        this.setupData.set(data);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Erreur lors du chargement des données de configuration');
        this.router.navigate(['/dashboard']);
      }
    });
  }

  nextStep(): void {
    if (this.currentStep() < 3) {
      this.currentStep.update(step => step + 1);
    }
  }

  toggleManualEntry(): void {
    this.showManualEntry.update(show => !show);
  }

  copySecret(): void {
    const secret = this.setupData()?.secret;
    if (secret) {
      navigator.clipboard.writeText(secret).then(() => {
        this.toast.success(this.t('mfa_setup_copied'));
      });
    }
  }

  copyAllBackupCodes(): void {
    const codes = this.setupData()?.backupCodes;
    if (codes) {
      const text = codes.join('\n');
      navigator.clipboard.writeText(text).then(() => {
        this.toast.success(this.t('mfa_setup_copied'));
      });
    }
  }

  copyBackupCode(code: string): void {
    navigator.clipboard.writeText(code).then(() => {
      this.toast.success(this.t('mfa_setup_copied'));
    });
  }

  downloadBackupCodes(): void {
    const codes = this.setupData()?.backupCodes;
    if (!codes) return;
    
    const date = new Date().toLocaleDateString();
    const content = `BankOS — ${this.i18n.lang() === 'fr' ? 'Codes de secours' : 'Backup codes'}\n${date}\n\n${codes.join('\n')}`;
    
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'bankos-backup-codes.txt';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  onVerificationInput(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/\D/g, '');
    
    if (value.length > 1) {
      value = value.slice(0, 1);
    }
    
    const newCode = [...this.verificationCode()];
    newCode[index] = value;
    this.verificationCode.set(newCode);
    
    // Clear error
    this.verificationError.set('');
    
    // Auto-focus next input
    if (value && index < 5) {
      const nextInput = document.querySelector(`#verify-digit-${index + 1}`) as HTMLInputElement;
      nextInput?.focus();
    }
    
    // Auto-verify when all digits filled
    if (newCode.every(d => d) && newCode.length === 6) {
      setTimeout(() => this.verifySetup(), 100);
    }
  }

  onVerificationKeyDown(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace') {
      const currentCode = this.verificationCode();
      if (!currentCode[index] && index > 0) {
        const newCode = [...currentCode];
        newCode[index - 1] = '';
        this.verificationCode.set(newCode);
        
        const prevInput = document.querySelector(`#verify-digit-${index - 1}`) as HTMLInputElement;
        prevInput?.focus();
      }
    }
  }

  verifySetup(): void {
    if (this.loading()) return;
    
    const code = this.verificationCode().join('');
    if (code.length !== 6) return;
    
    this.loading.set(true);
    this.verificationError.set('');
    
    this.auth.mfaSetupVerify(code).subscribe({
      next: () => {
        this.loading.set(false);
        this.nextStep();
      },
      error: () => {
        this.loading.set(false);
        this.verificationError.set(this.t('mfa_setup_invalid'));
        // Clear inputs and refocus
        this.verificationCode.set(['', '', '', '', '', '']);
        setTimeout(() => {
          const firstInput = document.querySelector('#verify-digit-0') as HTMLInputElement;
          firstInput?.focus();
        }, 50);
      }
    });
  }

  finishSetup(): void {
    if (!this.backupCodesConfirmed()) return;
    
    this.toast.success(this.t('mfa_setup_success'));
    
    // Navigate to dashboard
    const dashboardRoute = this.auth.redirectByRole();
    this.router.navigate([dashboardRoute]);
  }
}