import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Client, ClientRequest, CustomerService, Operateur } from '../../core/services/customer.service';

@Component({
  selector: 'app-clients',
  imports: [FormsModule],
  templateUrl: './clients.html',
  styleUrl: './clients.scss',
})
export class Clients implements OnInit {
  private customer = inject(CustomerService);

  clients = signal<Client[]>([]);
  operateurs = signal<Operateur[]>([]);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);

  // formulaire de creation
  form: ClientRequest = this.vide();

  ngOnInit(): void {
    this.charger();
    this.customer.getOperateurs().subscribe({ next: (o) => this.operateurs.set(o) });
  }

  charger(): void {
    this.customer.getClients().subscribe({
      next: (c) => this.clients.set(c),
      error: () => this.erreur.set('Impossible de charger les clients.'),
    });
  }

  creer(): void {
    this.erreur.set(null);
    this.succes.set(null);
    this.customer.createClient(this.form).subscribe({
      next: () => {
        this.succes.set('Client créé avec succès.');
        this.form = this.vide();
        this.charger();
      },
      error: (e) => this.erreur.set(e.status === 409 ? 'Cet email est déjà utilisé.' : 'Erreur lors de la création.'),
    });
  }

  changerKyc(c: Client, statut: string): void {
    this.customer.majKyc(c.id, statut).subscribe({
      next: () => this.charger(),
      error: () => this.erreur.set('Erreur lors de la mise à jour du KYC.'),
    });
  }

  badgeClass(statut: string): string {
    const suffixe = statut === 'VALIDE' ? 'valide' : statut === 'REJETE' ? 'rejete' : 'attente';
    return `badge badge-${suffixe}`;
  }

  private vide(): ClientRequest {
    return {
      operateurId: 0,
      nom: '',
      prenom: '',
      dateNaissance: '',
      email: '',
      telephone: '',
      numeroIdentite: '',
      typePiece: 'CNI',
      adresse: { rue: '', ville: '', pays: 'Cameroun', codePostal: '' },
    };
  }
}
