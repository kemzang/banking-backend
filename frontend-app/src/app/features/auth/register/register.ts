import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { I18nService } from '../../../core/services/i18n.service';
import { ThemeService } from '../../../core/services/theme.service';
import { AuthLayoutComponent } from '../../../shared/auth-layout/auth-layout';

interface FormData {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
}

interface ValidationErrors {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  password?: string;
  confirmPassword?: string;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AuthLayoutComponent],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
  private auth   = inject(AuthService);
  private router = inject(Router);
  private toast  = inject(ToastService);
  readonly i18n  = inject(I18nService);
  readonly theme = inject(ThemeService);

  loading = signal(false);
  
  form = signal<FormData>({
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: ''
  });

  showPassword = signal(false);
  showConfirmPassword = signal(false);
  errors = signal<ValidationErrors>({});

  // Password strength calculation
  passwordStrength = computed(() => {
    const pwd = this.form().password;
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
    const pwd = this.form().password;
    return [
      { key: 'length', label: this.t('pwd_req_length'), met: pwd.length >= 8 },
      { key: 'uppercase', label: this.t('pwd_req_upper'), met: /[A-Z]/.test(pwd) },
      { key: 'number', label: this.t('pwd_req_number'), met: /[0-9]/.test(pwd) },
      { key: 'special', label: this.t('pwd_req_special'), met: /[^A-Za-z0-9]/.test(pwd) }
    ];
  });

  t(key: Parameters<I18nService['t']>[0], vars?: Record<string, string>) { 
    return this.i18n.t(key, vars); 
  }

  updateForm(field: keyof FormData, value: string): void {
    this.form.update(current => ({ ...current, [field]: value }));
    // Clear error for this field
    this.errors.update(current => {
      const newErrors = { ...current };
      delete newErrors[field];
      return newErrors;
    });
  }

  validateForm(): boolean {
    const data = this.form();
    const newErrors: ValidationErrors = {};

    if (!data.firstName.trim()) {
      newErrors.firstName = this.t('required');
    }

    if (!data.lastName.trim()) {
      newErrors.lastName = this.t('required');
    }

    if (!data.email.trim()) {
      newErrors.email = this.t('required');
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)) {
      newErrors.email = 'Email invalide';
    }

    if (!data.phone.trim()) {
      newErrors.phone = this.t('required');
    } else if (!/^\+237[0-9]{9}$/.test(data.phone)) {
      newErrors.phone = this.t('phone_hint');
    }

    if (!data.password) {
      newErrors.password = this.t('required');
    } else if (this.passwordStrength().score < 3) {
      newErrors.password = 'Mot de passe trop faible';
    }

    if (!data.confirmPassword) {
      newErrors.confirmPassword = this.t('required');
    } else if (data.password !== data.confirmPassword) {
      newErrors.confirmPassword = this.t('passwords_no_match');
    }

    this.errors.set(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  onSubmit(): void {
    if (this.loading() || !this.validateForm()) return;
    
    this.loading.set(true);
    const data = this.form();
    
    this.auth.register({
      email: data.email,
      motDePasse: data.password,
      nom: data.lastName,
      prenom: data.firstName,
      telephone: data.phone
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/auth/client'], { 
          queryParams: { registered: 'true' } 
        });
      },
      error: (error) => {
        this.loading.set(false);
        if (error.status === 409) {
          this.errors.update(current => ({
            ...current,
            email: this.i18n.lang() === 'fr' ? 'Cet email est déjà utilisé' : 'This email is already taken'
          }));
        } else {
          this.toast.error('Erreur lors de l\'inscription');
        }
      }
    });
  }
}
