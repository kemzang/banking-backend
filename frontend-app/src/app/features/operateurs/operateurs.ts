import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CustomerService, Operateur } from '../../core/services/customer.service';

@Component({
  selector: 'app-operateurs',
  imports: [FormsModule],
  templateUrl: './operateurs.html',
})
export class Operateurs implements OnInit {
  private customer = inject(CustomerService);

  operateurs = signal<Operateur[]>([]);
  erreur = signal<string | null>(null);

  nom = '';
  type = 'BANQUE';
  code = '';

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.customer.getOperateurs().subscribe({
      next: (o) => this.operateurs.set(o),
      error: () => this.erreur.set('Impossible de charger les opérateurs.'),
    });
  }

  creer(): void {
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
}
