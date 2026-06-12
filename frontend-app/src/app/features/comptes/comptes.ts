import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { AccountService, Compte, CompteRequest } from '../../core/services/account.service';
import { Client, CustomerService } from '../../core/services/customer.service';

@Component({
  selector: 'app-comptes',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './comptes.html',
})
export class Comptes implements OnInit {
  private account = inject(AccountService);
  private customer = inject(CustomerService);

  comptes = signal<Compte[]>([]);
  clients = signal<Client[]>([]);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);

  form: CompteRequest = this.vide();

  ngOnInit(): void {
    this.charger();
    this.customer.getClients().subscribe({ next: (c) => this.clients.set(c) });
  }

  charger(): void {
    this.account.list().subscribe({
      next: (c) => this.comptes.set(c),
      error: () => this.erreur.set('Impossible de charger les comptes.'),
    });
  }

  ouvrir(): void {
    this.erreur.set(null);
    this.succes.set(null);
    this.account.open(this.form).subscribe({
      next: (c) => {
        this.succes.set(`Compte ${c.numeroCompte} ouvert.`);
        this.form = this.vide();
        this.charger();
      },
      error: () => this.erreur.set("Erreur lors de l'ouverture du compte."),
    });
  }

  suspendre(c: Compte): void {
    this.account.suspend(c.id).subscribe({ next: () => this.charger(), error: () => this.erreur.set('Erreur suspension.') });
  }

  cloturer(c: Compte): void {
    this.account.close(c.id).subscribe({
      next: () => this.charger(),
      error: (e) => this.erreur.set(e.status === 409 ? 'Le solde doit être à 0 pour clôturer.' : 'Erreur clôture.'),
    });
  }

  nomClient(id: number): string {
    const c = this.clients().find((x) => x.id === id);
    return c ? `${c.prenom} ${c.nom}` : `#${id}`;
  }

  badgeClass(statut: string): string {
    const s = statut === 'ACTIF' ? 'valide' : statut === 'CLOTURE' ? 'rejete' : 'attente';
    return `badge badge-${s}`;
  }

  private vide(): CompteRequest {
    return { clientId: 0, operateurId: 1, type: 'COURANT', devise: 'XAF', plafondJournalier: 500000, decouvertAutorise: 0 };
  }
}
