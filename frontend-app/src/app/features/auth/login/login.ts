import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  motDePasse = '';
  erreur = signal<string | null>(null);
  chargement = signal(false);

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
}
