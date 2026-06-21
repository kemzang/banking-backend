import { AfterViewInit, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

declare const google: any;

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login implements AfterViewInit {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  motDePasse = '';
  erreur = signal<string | null>(null);
  chargement = signal(false);
  googleActif = !!environment.googleClientId;

  seConnecter(): void {
    this.erreur.set(null);
    this.chargement.set(true);
    this.auth.login({ email: this.email, motDePasse: this.motDePasse }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (e) => {
        this.erreur.set(e.status === 401 ? 'Email ou mot de passe incorrect.' : 'Erreur de connexion au serveur.');
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
      const el = document.getElementById('googleBtn');
      if (el) {
        google.accounts.id.renderButton(el, { theme: 'outline', size: 'large', width: 320 });
      }
    } else {
      setTimeout(() => this.initGoogleButton(), 500);
    }
  }

  connexionGoogleIndisponible(): void {
    this.erreur.set('Configurez GOOGLE_CLIENT_ID (.env + environment.ts) pour activer la connexion Google.');
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
