import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CustomerService, Client } from '../../../core/services/customer.service';
import { AccountService, Compte } from '../../../core/services/account.service';
import { TransactionService, Transaction } from '../../../core/services/transaction.service';
import { LoanService, DemandePret } from '../../../core/services/loan.service';

@Component({
  selector: 'app-client-dashboard',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './client-dashboard.html',
  styleUrl: './client-dashboard.scss',
})
export class ClientDashboard implements OnInit {
  private auth = inject(AuthService);
  private customers = inject(CustomerService);
  private accountsApi = inject(AccountService);
  private transactionsApi = inject(TransactionService);
  private loansApi = inject(LoanService);

  profile = signal<Client | null>(null);
  accounts = signal<Compte[]>([]);
  transactions = signal<Transaction[]>([]);
  demandes = signal<DemandePret[]>([]);
  chargement = signal(true);

  ngOnInit(): void {
    this.customers.getClientParEmail(this.auth.email()).subscribe({ next: profile => this.profile.set(profile) });
    this.loansApi.mesDemandes().subscribe({ next: demandes => this.demandes.set(demandes.slice(0, 5)) });
    this.accountsApi.list().pipe(
      switchMap(accounts => {
        this.accounts.set(accounts);
        return accounts.length
          ? forkJoin(accounts.map(account => this.transactionsApi.byAccount(account.id)))
          : of([] as Transaction[][]);
      }),
    ).subscribe({
      next: groups => {
        this.transactions.set(groups.flat().sort((a, b) => b.dateOperation.localeCompare(a.dateOperation)).slice(0, 5));
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }

  soldeTotal(): number { return this.accounts().reduce((total, account) => total + Number(account.solde), 0); }
}
