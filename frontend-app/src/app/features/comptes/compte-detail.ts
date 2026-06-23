import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AccountService, Compte } from '../../core/services/account.service';
import { Transaction, TransactionService } from '../../core/services/transaction.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-compte-detail',
  imports: [DecimalPipe, DatePipe, RouterLink],
  templateUrl: './compte-detail.html',
  styleUrl: './compte-detail.scss',
})
export class CompteDetail implements OnInit {
  private route = inject(ActivatedRoute);
  private accountService = inject(AccountService);
  private transactionService = inject(TransactionService);
  private auth = inject(AuthService);

  compte = signal<Compte | null>(null);
  transactions = signal<Transaction[]>([]);
  chargement = signal(false);
  chargementHistorique = signal(false);
  erreur = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.erreur.set('Identifiant de compte invalide.');
      return;
    }
    this.chargerCompte(id);
    this.chargerHistorique(id);
  }

  chargerCompte(id: number): void {
    this.chargement.set(true);
    this.accountService.getById(id).subscribe({
      next: (compte) => this.compte.set(compte),
      error: () => this.erreur.set('Impossible de charger le compte.'),
      complete: () => this.chargement.set(false),
    });
  }

  chargerHistorique(id: number): void {
    this.chargementHistorique.set(true);
    this.transactionService.getTransactionsByAccountId(id).subscribe({
      next: (transactions) => this.transactions.set(transactions),
      error: (e) => this.erreur.set(e?.message || 'Impossible de charger l’historique.'),
      complete: () => this.chargementHistorique.set(false),
    });
  }

  badgeCompte(statut: string): string {
    const s = statut === 'ACTIF' ? 'valide' : statut === 'CLOTURE' ? 'rejete' : 'attente';
    return `badge badge-${s}`;
  }

  badgeTransaction(statut: string): string {
    const s = statut === 'VALIDEE' ? 'valide' : statut === 'REJETEE' ? 'rejete' : 'attente';
    return `badge badge-${s}`;
  }

  ligneRejetee(transaction: Transaction): boolean {
    return transaction.statut === 'REJETEE';
  }

  compteLabel(id?: number | null): string {
    return id ? `#${id}` : '-';
  }

  accountsLink(): string { return this.auth.hasRole('CLIENT') ? '/client/accounts' : '/comptes'; }
  transactionsLink(): string { return this.auth.hasRole('CLIENT') ? '/client/transactions' : '/transactions'; }
}
