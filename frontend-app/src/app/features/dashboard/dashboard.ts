import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { CustomerService, Client } from '../../core/services/customer.service';
import { DocumentService } from '../../core/services/document.service';
import { AccountService, Compte } from '../../core/services/account.service';
import { TransactionService, Transaction } from '../../core/services/transaction.service';
import { LoanService, Pret } from '../../core/services/loan.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private auth = inject(AuthService);
  private customer = inject(CustomerService);
  private docs = inject(DocumentService);
  private account = inject(AccountService);
  private transaction = inject(TransactionService);
  private loan = inject(LoanService);

  // Stats globales (OPERATEUR)
  nbClients = signal(0);
  nbComptes = signal(0);
  nbOperateurs = signal(0);
  nbDocuments = signal(0);

  // Données personnelles (CLIENT)
  client = signal<Client | null>(null);
  mesComptes = signal<Compte[]>([]);
  mesTransactions = signal<Transaction[]>([]);
  mesPrets = signal<Pret[]>([]);

  estClient = false;
  estOperateur = false;

  ngOnInit(): void {
    this.estClient = this.auth.hasRole('CLIENT');
    this.estOperateur = this.auth.hasRole('OPERATEUR') || this.auth.hasRole('ADMIN');

    if (this.estClient) {
      this.chargerDonneesClient();
    } else {
      this.chargerStatsGlobales();
    }
  }

  private chargerStatsGlobales(): void {
    this.customer.getClients().subscribe({ next: (c) => this.nbClients.set(c.length) });
    this.customer.getOperateurs().subscribe({ next: (o) => this.nbOperateurs.set(o.length) });
    this.account.list().subscribe({ next: (a) => this.nbComptes.set(a.length) });
    this.docs.history().subscribe({ next: (h) => this.nbDocuments.set(h.length) });
  }

  private chargerDonneesClient(): void {
    const email = this.auth.email();
    if (!email) return;

    this.customer.getClientParEmail(email).subscribe({
      next: (c) => {
        this.client.set(c);
        this.account.list(c.id).subscribe({
          next: (comptes) => {
            this.mesComptes.set(comptes);
            // Charger transactions et prêts pour chaque compte
            comptes.forEach((compte) => {
              this.transaction.byAccount(compte.id).subscribe({
                next: (txs) => {
                  const actuelles = this.mesTransactions();
                  this.mesTransactions.set([...actuelles, ...txs]);
                },
              });
            });
          },
        });
      },
    });
  }

  soldeTotal(): number {
    return this.mesComptes().reduce((sum, c) => sum + c.solde, 0);
  }

  nbTransactions(): number {
    return this.mesTransactions().length;
  }

  nbPrets(): number {
    return this.mesPrets().length;
  }
}
