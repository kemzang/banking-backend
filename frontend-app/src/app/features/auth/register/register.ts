import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: '../login/login.scss',
})
export class Register {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  motDePasse = '';
  telephone = '';
  erreur = signal<string | null>(null);
  chargement = signal(false);

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
}
