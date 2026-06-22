import {
  AfterViewInit, Component, ElementRef, EventEmitter,
  Input, OnDestroy, OnInit, Output, QueryList,
  ViewChildren, inject, signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-mfa-modal',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './mfa-modal.html',
  styleUrl: './mfa-modal.scss',
})
export class MfaModalComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() email = '';
  @Output() verified  = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  @ViewChildren('otpInput') otpInputs!: QueryList<ElementRef<HTMLInputElement>>;

  private auth  = inject(AuthService);
  private toast = inject(ToastService);

  // TOTP state
  digits       = signal<string[]>(['', '', '', '', '', '']);
  chargement   = signal(false);
  erreur       = signal<string | null>(null);
  shake        = signal(false);

  // Timer 30s
  countdown    = signal(30);
  resendActive = signal(false);
  private timerRef?: ReturnType<typeof setInterval>;

  // Mode: totp | backup
  mode         = signal<'totp' | 'backup'>('totp');
  backupCode   = signal('');

  ngOnInit(): void { this.startCountdown(); }

  ngAfterViewInit(): void {
    setTimeout(() => this.otpInputs.first?.nativeElement.focus(), 50);
  }

  ngOnDestroy(): void { clearInterval(this.timerRef); }

  private startCountdown(): void {
    clearInterval(this.timerRef);
    this.countdown.set(30);
    this.resendActive.set(false);
    this.timerRef = setInterval(() => {
      const n = this.countdown() - 1;
      if (n <= 0) {
        this.countdown.set(0);
        this.resendActive.set(true);
        clearInterval(this.timerRef);
      } else {
        this.countdown.set(n);
      }
    }, 1000);
  }

  // ── OTP input handlers ─────────────────────────────────

  onDigitInput(index: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const val   = input.value.replace(/\D/g, '').slice(-1);
    const arr   = [...this.digits()];
    arr[index]  = val;
    this.digits.set(arr);
    if (val && index < 5) {
      this.otpInputs.get(index + 1)?.nativeElement.focus();
    }
  }

  onKeyDown(index: number, event: KeyboardEvent): void {
    if (event.key === 'Backspace') {
      const arr = [...this.digits()];
      if (arr[index]) {
        arr[index] = '';
        this.digits.set(arr);
      } else if (index > 0) {
        this.otpInputs.get(index - 1)?.nativeElement.focus();
      }
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const nums  = text.replace(/\D/g, '').slice(0, 6).split('');
    const arr   = ['', '', '', '', '', ''];
    nums.forEach((d, i) => { arr[i] = d; });
    this.digits.set(arr);
    const focusIdx = Math.min(nums.length, 5);
    this.otpInputs.get(focusIdx)?.nativeElement.focus();
  }

  get otpCode(): string { return this.digits().join(''); }

  private clearDigits(): void {
    this.digits.set(['', '', '', '', '', '']);
    setTimeout(() => this.otpInputs.first?.nativeElement.focus(), 50);
  }

  private triggerShake(): void {
    this.shake.set(true);
    setTimeout(() => this.shake.set(false), 600);
  }

  // ── Verification ───────────────────────────────────────

  verify(): void {
    if (this.chargement()) return;
    this.erreur.set(null);
    this.chargement.set(true);

    if (this.mode() === 'backup') {
      this.auth.mfaVerifyBackup({ email: this.email, backupCode: this.backupCode().toUpperCase() }).subscribe({
        next: (res) => {
          this.chargement.set(false);
          if (res.backupCodesRemaining !== undefined) {
            this.toast.warning(`Code de secours utilisé — il vous reste ${res.backupCodesRemaining} code(s).`);
          }
          this.verified.emit();
        },
        error: () => {
          this.chargement.set(false);
          this.erreur.set('Code de secours invalide.');
          this.triggerShake();
        },
      });
      return;
    }

    if (this.otpCode.length !== 6) {
      this.chargement.set(false);
      this.erreur.set('Entrez les 6 chiffres du code.');
      return;
    }

    this.auth.mfaVerify({ email: this.email, code: this.otpCode }).subscribe({
      next: () => { this.chargement.set(false); this.verified.emit(); },
      error: () => {
        this.chargement.set(false);
        this.erreur.set('Code incorrect, veuillez réessayer.');
        this.triggerShake();
        this.clearDigits();
      },
    });
  }

  resend(): void {
    this.auth.mfaResend({ email: this.email }).subscribe({
      next: () => { this.toast.info('Nouveau code envoyé.'); this.startCountdown(); },
      error: () => this.toast.error('Impossible d\'envoyer le code.'),
    });
  }

  switchMode(m: 'totp' | 'backup'): void {
    this.mode.set(m);
    this.erreur.set(null);
    this.backupCode.set('');
    this.clearDigits();
  }
}
