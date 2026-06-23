import { Component, OnInit, inject, signal } from '@angular/core';
import { forkJoin, of, switchMap } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { CustomerService } from '../../../core/services/customer.service';
import { AccountService } from '../../../core/services/account.service';
import { TransactionService } from '../../../core/services/transaction.service';
import { UserResponse } from '../../../core/models/auth.models';

@Component({
  selector: 'app-operator-dashboard',
  standalone: true,
  templateUrl: './operator-dashboard.html',
  styleUrl: './operator-dashboard.scss',
})
export class OperatorDashboard implements OnInit {
  private auth = inject(AuthService);
  private customers = inject(CustomerService);
  private accounts = inject(AccountService);
  private transactions = inject(TransactionService);

  user = signal<UserResponse | null>(null);
  clients = signal(0);
  comptes = signal(0);
  operations = signal(0);

  ngOnInit(): void {
    this.auth.me().subscribe(user => this.user.set(user));
    this.customers.getClients().subscribe(clients => this.clients.set(clients.length));
    this.accounts.list().pipe(
      switchMap(accounts => {
        this.comptes.set(accounts.length);
        return accounts.length
          ? forkJoin(accounts.map(account => this.transactions.byAccount(account.id)))
          : of([]);
      }),
    ).subscribe(groups => this.operations.set(groups.reduce((total, group) => total + group.length, 0)));
  }
}
