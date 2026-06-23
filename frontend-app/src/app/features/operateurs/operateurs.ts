import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CustomerService, Operateur } from '../../core/services/customer.service';
import { AuthService } from '../../core/services/auth.service';
import { switchMap } from 'rxjs';

@Component({
  selector: 'app-operateurs',
  imports: [FormsModule],
  templateUrl: './operateurs.html',
  styleUrl: './operateurs.scss',
})
export class Operateurs implements OnInit {
  private customer = inject(CustomerService);
  private auth = inject(AuthService);

  operateurs = signal<Operateur[]>([]);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);
  creation = signal(false);

  nom = '';
  type = 'BANQUE';
  code = '';
  statut = 'ACTIVE';
  adminFirstName = '';
  adminLastName = '';
  adminEmail = '';
  adminPassword = '';
  private pendingOperatorId: number | null = null;

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
    this.succes.set(null);
    this.creation.set(true);
    if (this.pendingOperatorId !== null) {
      this.auth.createOperatorAdmin(this.adminPayload(this.pendingOperatorId)).subscribe({
        next: admin => this.creationReussie(admin),
        error: () => {
          this.creation.set(false);
          this.erreur.set(`L'opérateur #${this.pendingOperatorId} existe déjà. Corrigez les informations administrateur puis réessayez.`);
        },
      });
      return;
    }
    this.customer.createOperateur({
      nom: this.nom,
      type: this.type,
      code: this.code,
      statut: this.statut,
    }).pipe(
      switchMap(operator => {
        this.pendingOperatorId = operator.id;
        return this.auth.createOperatorAdmin(this.adminPayload(operator.id));
      }),
    ).subscribe({
      next: admin => this.creationReussie(admin),
      error: (e) => {
        this.creation.set(false);
        if (this.pendingOperatorId !== null) {
          this.erreur.set(`L'opérateur #${this.pendingOperatorId} a été créé, mais son OPERATOR_ADMIN a échoué. Corrigez les informations puis réessayez.`);
        } else {
          this.erreur.set(e.status === 409 ? 'Ce code est déjà utilisé.' : "Erreur lors de la création de l'opérateur.");
        }
      },
    });
  }

  private adminPayload(operatorId: number) {
    return {
      firstName: this.adminFirstName,
      lastName: this.adminLastName,
      email: this.adminEmail,
      password: this.adminPassword,
      operatorId,
    };
  }

  private creationReussie(admin: { email: string }): void {
    this.pendingOperatorId = null;
    this.resetForm();
    this.creation.set(false);
    this.succes.set(`Opérateur créé. Premier administrateur : ${admin.email}`);
    this.charger();
  }

  private resetForm(): void {
    this.nom = '';
    this.code = '';
    this.statut = 'ACTIVE';
    this.adminFirstName = '';
    this.adminLastName = '';
    this.adminEmail = '';
    this.adminPassword = '';
  }

  typeBadge(type: string): string {
    const map: Record<string, string> = {
      BANQUE: 'badge badge-encours',
      MICROFINANCE: 'badge badge-valide',
      MOBILE: 'badge badge-attente',
    };
    return map[type] ?? 'badge badge-inactive';
  }

  changerStatut(operateur: Operateur, status: string): void {
    this.customer.updateOperateurStatus(operateur.id, status).subscribe({
      next: updated => this.operateurs.update(items => items.map(item => item.id === updated.id ? updated : item)),
      error: () => this.erreur.set("Impossible de modifier le statut de l'opérateur."),
    });
  }
}
