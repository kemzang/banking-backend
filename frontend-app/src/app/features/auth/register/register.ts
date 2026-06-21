import { AfterViewInit, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

declare const google: any;

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: '../login/login.scss',
})
export class Register implements AfterViewInit {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  motDePasse = '';
  telephone = '';
  erreur = signal<string | null>(null);
  chargement = signal(false);
  googleActif = !!environment.googleClientId;

  sInscrire(): void {
    this.erreur.set(null);
    this.chargement.set(true);
    this.auth.register({ email: this.email, motDePasse: this.motDePasse, telephone: this.telephone }).subscribe({
      next: () => {
        // inscription OK -> on connecte automatiquement
        this.auth.login({ email: this.email, motDePasse: this.motDePasse }).subscribe({
          next: () => this.router.navigate(['/dashboard']),
          error: () => this.router.navigate(['/login']),
        });
      },
      error: (e) => {
        this.erreur.set(e.status === 409 ? 'Cet email est deja utilise.' : 'Erreur lors de l\'inscription.');
        this.chargement.set(false);
      },
    });
  }

  ngAfterViewInit(): void {
    if (!this.googleActif) return;
    this.initGoogleButton();
  }

  private initGoogleButton(): void {
    if (typeof google !== 'undefined' && google.accounts) {
      google.accounts.id.initialize({
        client_id: environment.googleClientId,
        callback: (resp: any) => this.onGoogle(resp),
      });
      const el = document.getElementById('googleBtnRegister');
      if (el) {
        google.accounts.id.renderButton(el, { theme: 'outline', size: 'large', width: 320, text: 'signup_with' });
      }
    } else {
      setTimeout(() => this.initGoogleButton(), 500);
    }
  }

  private onGoogle(resp: any): void {
    this.erreur.set(null);
    this.chargement.set(true);
    this.auth.googleLogin(resp.credential).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (e) => {
        const msg = e.status === 401
          ? 'Jeton Google invalide ou expiré. Réessayez.'
          : e.status === 503
            ? 'Connexion Google non configurée côté serveur.'
            : 'Erreur lors de la connexion Google. Réessayez.';
        this.erreur.set(msg);
        this.chargement.set(false);
      },
    });
  }
}
