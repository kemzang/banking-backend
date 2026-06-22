import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { AccountService, Compte } from '../../core/services/account.service';
import { CustomerService } from '../../core/services/customer.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-transactions',
  imports: [FormsModule, ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './transactions.html',
  styleUrl: './transactions.scss',
})
export class Transactions implements OnInit {
  private tx = inject(TransactionService);
  private account = inject(AccountService);
  private customer = inject(CustomerService);
  private auth = inject(AuthService);

  comptes = signal<Compte[]>([]);
  historique = signal<Transaction[]>([]);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);
  transactionCreee = signal<Transaction | null>(null);
  chargementComptes = signal(false);
  chargementHistorique = signal(false);
  operationEnCours = signal(false);

  depotForm = this.fb.nonNullable.group({
    compteId: [0, [Validators.required, Validators.min(1)]],
    montant: [0, [Validators.required, Validators.min(0.01)]],
    devise: ['XAF', [Validators.required]],
  });

  retraitForm = this.fb.nonNullable.group({
    compteId: [0, [Validators.required, Validators.min(1)]],
    montant: [0, [Validators.required, Validators.min(0.01)]],
    devise: ['XAF', [Validators.required]],
  });

  transfertForm = this.fb.nonNullable.group(
    {
      compteSourceId: [0, [Validators.required, Validators.min(1)]],
      compteDestId: [0, [Validators.required, Validators.min(1)]],
      montant: [0, [Validators.required, Validators.min(0.01)]],
      devise: ['XAF', [Validators.required]],
      motif: ['', [Validators.maxLength(500)]],
    },
    { validators: this.comptesDifferents },
  );

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
    this.reinitialiserMessages();
    if (this.depotForm.invalid) {
      this.depotForm.markAllAsTouched();
      this.erreur.set('Vérifiez le compte, le montant et la devise du dépôt.');
      return;
    }

    const payload: DepositRequest = this.depotForm.getRawValue();
    this.operationEnCours.set(true);
    this.tx.deposit(payload).subscribe({
      next: (transaction) => {
        this.transactionCreee.set(transaction);
        this.depotForm.reset({ compteId: 0, montant: 0, devise: payload.devise });
        this.apres('Dépôt effectué.');
      },
      error: (e) => this.echec(e),
    });
  }

  retirer(): void {
    this.reinitialiserMessages();
    if (this.retraitForm.invalid) {
      this.retraitForm.markAllAsTouched();
      this.erreur.set('Vérifiez le compte, le montant et la devise du retrait.');
      return;
    }

    const payload: WithdrawRequest = this.retraitForm.getRawValue();
    this.operationEnCours.set(true);
    this.tx.withdraw(payload).subscribe({
      next: (transaction) => {
        this.transactionCreee.set(transaction);
        this.retraitForm.reset({ compteId: 0, montant: 0, devise: payload.devise });
        this.apres('Retrait effectué.');
      },
      error: (e) => this.echec(e),
    });
  }

  transferer(): void {
    this.reinitialiserMessages();
    if (this.transfertForm.invalid) {
      this.transfertForm.markAllAsTouched();
      this.erreur.set('Vérifiez le compte source, le compte destination, le montant et la devise du transfert.');
      return;
    }

    const payload: TransferRequest = this.transfertForm.getRawValue();
    this.operationEnCours.set(true);
    this.tx.transfer(payload).subscribe({
      next: (transaction) => {
        this.transactionCreee.set(transaction);
        this.compteHisto = payload.compteSourceId;
        this.transfertForm.reset({
          compteSourceId: 0,
          compteDestId: 0,
          montant: 0,
          devise: payload.devise,
          motif: '',
        });
        this.apres(`Transfert effectué. Référence : ${transaction.reference}`);
      },
      error: (e) => this.echec(e),
    });
  }

  chargerHistorique(): void {
    if (!this.compteHisto) return;
    this.erreur.set(null);
    this.chargementHistorique.set(true);
    this.tx.getTransactionsByAccountId(this.compteHisto).subscribe({
      next: (h) => {
        this.historique.set(h);
        this.chargementHistorique.set(false);
      },
      error: (e) => this.echec(e),
    });
  }

  badge(statut: string): string {
    const s = statut === 'VALIDEE' ? 'valide' : statut === 'REJETEE' ? 'rejete' : 'attente';
    return `badge badge-${s}`;
  }

  compteLabel(id?: number | null): string {
    if (!id) return '-';
    const compte = this.comptes().find((c) => c.id === id);
    return compte ? compte.numeroCompte : `Compte #${id}`;
  }

  ligneRejetee(transaction: Transaction): boolean {
    return transaction.statut === 'REJETEE';
  }

  depotInvalide(champ: 'compteId' | 'montant' | 'devise'): boolean {
    return this.champInvalide(this.depotForm.controls[champ]);
  }

  retraitInvalide(champ: 'compteId' | 'montant' | 'devise'): boolean {
    return this.champInvalide(this.retraitForm.controls[champ]);
  }

  transfertInvalide(champ: 'compteSourceId' | 'compteDestId' | 'montant' | 'devise' | 'motif'): boolean {
    return this.champInvalide(this.transfertForm.controls[champ]);
  }

  transfertMemeCompte(): boolean {
    return this.transfertForm.hasError('sameAccount') && (this.transfertForm.dirty || this.transfertForm.touched);
  }

  private apres(msg: string): void {
    this.succes.set(msg);
    this.erreur.set(null);
    this.operationEnCours.set(false);
    this.chargerComptes();
    if (this.compteHisto) this.chargerHistorique();
  }

  private echec(e: any): void {
    this.succes.set(null);
    this.operationEnCours.set(false);
    this.chargementHistorique.set(false);
    this.erreur.set(e?.message || 'Opération refusée (vérifiez le solde / les comptes).');
  }

  private reinitialiserMessages(): void {
    this.erreur.set(null);
    this.succes.set(null);
    this.transactionCreee.set(null);
  }

  private champInvalide(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }

  private comptesDifferents(control: AbstractControl): ValidationErrors | null {
    const source = control.get('compteSourceId')?.value;
    const destination = control.get('compteDestId')?.value;
    return source && destination && source === destination ? { sameAccount: true } : null;
  }
}
