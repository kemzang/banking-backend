import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CustomerService, Operateur } from '../../core/services/customer.service';
import { UserResponse } from '../../core/models/auth.models';

@Component({
  selector: 'app-dashboard',
  imports: [FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private auth = inject(AuthService);
  private customer = inject(CustomerService);
  private router = inject(Router);

  user = signal<UserResponse | null>(null);
  operateurs = signal<Operateur[]>([]);
  erreur = signal<string | null>(null);

  // formulaire de creation d'operateur
  nom = '';
  type = 'BANQUE';
  code = '';

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.auth.me().subscribe({ next: (u) => this.user.set(u) });
    this.customer.getOperateurs().subscribe({
      next: (o) => this.operateurs.set(o),
      error: () => this.erreur.set('Impossible de charger les opérateurs.'),
    });
  }

  ajouterOperateur(): void {
    this.erreur.set(null);
    this.customer.createOperateur({ nom: this.nom, type: this.type, code: this.code }).subscribe({
      next: () => {
        this.nom = '';
        this.code = '';
        this.charger();
      },
      error: (e) => this.erreur.set(e.status === 409 ? 'Ce code est déjà utilisé.' : 'Erreur lors de la création.'),
    });
  }

  deconnexion(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
