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
    if (!this.googleActif || typeof google === 'undefined') return;
    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (resp: any) => this.onGoogle(resp),
    });
    const el = document.getElementById('googleBtn');
    if (el) {
      google.accounts.id.renderButton(el, { theme: 'outline', size: 'large', width: 320 });
    }
  }

  private onGoogle(resp: any): void {
    this.auth.googleLogin(resp.credential).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => this.erreur.set('Connexion Google refusée.'),
    });
  }
}
