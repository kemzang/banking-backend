import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { TransactionService, Transaction } from '../../core/services/transaction.service';
import { AccountService, Compte } from '../../core/services/account.service';

@Component({
  selector: 'app-transactions',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './transactions.html',
})
export class Transactions implements OnInit {
  private tx = inject(TransactionService);
  private account = inject(AccountService);

  comptes = signal<Compte[]>([]);
  historique = signal<Transaction[]>([]);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);

  depot = { compteId: 0, montant: 0, devise: 'XAF' };
  retrait = { compteId: 0, montant: 0, devise: 'XAF' };
  transfert = { compteSourceId: 0, compteDestId: 0, montant: 0, devise: 'XAF', motif: '' };
  compteHisto = 0;

  ngOnInit(): void {
    this.chargerComptes();
  }

  chargerComptes(): void {
    this.account.list().subscribe({ next: (c) => this.comptes.set(c) });
  }

  private apres(msg: string): void {
    this.succes.set(msg);
    this.erreur.set(null);
    this.chargerComptes();
    if (this.compteHisto) this.chargerHistorique();
  }
  private echec(e: any): void {
    this.succes.set(null);
    this.erreur.set(e?.error?.error || e?.error?.message || 'Opération refusée (vérifiez le solde / les comptes).');
  }

  deposer(): void {
    this.tx.deposit(this.depot).subscribe({ next: () => this.apres('Dépôt effectué.'), error: (e) => this.echec(e) });
  }
  retirer(): void {
    this.tx.withdraw(this.retrait).subscribe({ next: () => this.apres('Retrait effectué.'), error: (e) => this.echec(e) });
  }
  transferer(): void {
    this.tx.transfer(this.transfert).subscribe({ next: () => this.apres('Transfert effectué.'), error: (e) => this.echec(e) });
  }
  chargerHistorique(): void {
    if (!this.compteHisto) return;
    this.tx.byAccount(this.compteHisto).subscribe({ next: (h) => this.historique.set(h) });
  }

  badge(statut: string): string {
    const s = statut === 'VALIDEE' ? 'valide' : statut === 'REJETEE' ? 'rejete' : 'attente';
    return `badge badge-${s}`;
  }
}
