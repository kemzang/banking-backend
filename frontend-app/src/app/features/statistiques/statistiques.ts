import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { CustomerService } from '../../core/services/customer.service';
import { AccountService, Compte } from '../../core/services/account.service';
import { NotificationService } from '../../core/services/notification.service';

interface StatType { type: string; nb: number; total: number; }

@Component({
  selector: 'app-statistiques',
  imports: [DecimalPipe],
  templateUrl: './statistiques.html',
})
export class Statistiques implements OnInit {
  private customer = inject(CustomerService);
  private account = inject(AccountService);
  private notif = inject(NotificationService);

  nbClients = signal(0);
  nbOperateurs = signal(0);
  nbComptes = signal(0);
  nbNotifs = signal(0);
  soldeTotal = signal(0);
  parType = signal<StatType[]>([]);
  parStatutKyc = signal<{ statut: string; nb: number }[]>([]);

  ngOnInit(): void {
    this.customer.getClients().subscribe({
      next: (clients) => {
        this.nbClients.set(clients.length);
        const map = new Map<string, number>();
        clients.forEach((c) => map.set(c.statutKyc, (map.get(c.statutKyc) ?? 0) + 1));
        this.parStatutKyc.set([...map].map(([statut, nb]) => ({ statut, nb })));
      },
    });
    this.customer.getOperateurs().subscribe({ next: (o) => this.nbOperateurs.set(o.length) });
    this.notif.list().subscribe({ next: (n) => this.nbNotifs.set(n.length) });
    this.account.list().subscribe({ next: (comptes) => this.calculComptes(comptes) });
  }

  private calculComptes(comptes: Compte[]): void {
    this.nbComptes.set(comptes.length);
    this.soldeTotal.set(comptes.reduce((s, c) => s + Number(c.solde), 0));
    const map = new Map<string, StatType>();
    comptes.forEach((c) => {
      const e = map.get(c.type) ?? { type: c.type, nb: 0, total: 0 };
      e.nb += 1;
      e.total += Number(c.solde);
      map.set(c.type, e);
    });
    this.parType.set([...map.values()]);
  }
}
