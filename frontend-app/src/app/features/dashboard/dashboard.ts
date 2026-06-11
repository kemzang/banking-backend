import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CustomerService } from '../../core/services/customer.service';
import { DocumentService } from '../../core/services/document.service';
import { AccountService } from '../../core/services/account.service';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private customer = inject(CustomerService);
  private docs = inject(DocumentService);
  private account = inject(AccountService);

  nbClients = signal(0);
  nbComptes = signal(0);
  nbOperateurs = signal(0);
  nbDocuments = signal(0);

  ngOnInit(): void {
    this.customer.getClients().subscribe({ next: (c) => this.nbClients.set(c.length) });
    this.customer.getOperateurs().subscribe({ next: (o) => this.nbOperateurs.set(o.length) });
    this.account.list().subscribe({ next: (a) => this.nbComptes.set(a.length) });
    this.docs.history().subscribe({ next: (h) => this.nbDocuments.set(h.length) });
  }
}
