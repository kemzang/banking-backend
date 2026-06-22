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
import {
  DepositRequest,
  Transaction,
  TransactionService,
  TransferRequest,
  WithdrawRequest,
} from '../../core/services/transaction.service';

@Component({
  selector: 'app-transactions',
  imports: [FormsModule, ReactiveFormsModule, DecimalPipe, DatePipe],
  templateUrl: './transactions.html',
  styleUrl: './transactions.scss',
})
export class Transactions implements OnInit {
  private tx = inject(TransactionService);
  private account = inject(AccountService);
  private fb = inject(FormBuilder);

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
    montant:  [0, [Validators.required, Validators.min(0.01)]],
    devise:   ['XAF', [Validators.required]],
  });

  retraitForm = this.fb.nonNullable.group({
    compteId: [0, [Validators.required, Validators.min(1)]],
    montant:  [0, [Validators.required, Validators.min(0.01)]],
    devise:   ['XAF', [Validators.required]],
  });

  transfertForm = this.fb.nonNullable.group(
    {
      compteSourceId: [0, [Validators.required, Validators.min(1)]],
      compteDestId:   [0, [Validators.required, Validators.min(1)]],
      montant:        [0, [Validators.required, Validators.min(0.01)]],
      devise:         ['XAF', [Validators.required]],
      motif:          ['', [Validators.maxLength(500)]],
    },
    { validators: comptesDifferents },
  );

  compteHisto = 0;

  ngOnInit(): void {
    this.chargerComptes();
  }

  chargerComptes(): void {
    this.chargementComptes.set(true);
    this.account.list().subscribe({
      next: (c) => { this.comptes.set(c); this.chargementComptes.set(false); },
      error: () => { this.chargementComptes.set(false); },
    });
  }

  deposer(): void {
    this.reset();
    if (this.depotForm.invalid) {
      this.depotForm.markAllAsTouched();
      this.erreur.set('Vérifiez le compte, le montant et la devise du dépôt.');
      return;
    }
    const payload = this.depotForm.getRawValue() as DepositRequest;
    this.operationEnCours.set(true);
    this.tx.deposit(payload).subscribe({
      next: (t) => {
        this.transactionCreee.set(t);
        this.depotForm.reset({ compteId: 0, montant: 0, devise: payload.devise });
        this.apres('Dépôt effectué.');
      },
      error: (e) => this.echec(e),
    });
  }

  retirer(): void {
    this.reset();
    if (this.retraitForm.invalid) {
      this.retraitForm.markAllAsTouched();
      this.erreur.set('Vérifiez le compte, le montant et la devise du retrait.');
      return;
    }
    const payload = this.retraitForm.getRawValue() as WithdrawRequest;
    this.operationEnCours.set(true);
    this.tx.withdraw(payload).subscribe({
      next: (t) => {
        this.transactionCreee.set(t);
        this.retraitForm.reset({ compteId: 0, montant: 0, devise: payload.devise });
        this.apres('Retrait effectué.');
      },
      error: (e) => this.echec(e),
    });
  }

  transferer(): void {
    this.reset();
    if (this.transfertForm.invalid) {
      this.transfertForm.markAllAsTouched();
      this.erreur.set('Vérifiez les comptes, le montant et la devise du transfert.');
      return;
    }
    const payload = this.transfertForm.getRawValue() as TransferRequest;
    this.operationEnCours.set(true);
    this.tx.transfer(payload).subscribe({
      next: (t) => {
        this.transactionCreee.set(t);
        this.compteHisto = payload.compteSourceId;
        this.transfertForm.reset({ compteSourceId: 0, compteDestId: 0, montant: 0, devise: payload.devise, motif: '' });
        this.apres(`Transfert effectué. Référence : ${t.reference}`);
      },
      error: (e) => this.echec(e),
    });
  }

  chargerHistorique(): void {
    if (!this.compteHisto) return;
    this.erreur.set(null);
    this.chargementHistorique.set(true);
    this.tx.getTransactionsByAccountId(this.compteHisto).subscribe({
      next: (h) => { this.historique.set(h); this.chargementHistorique.set(false); },
      error: (e) => this.echec(e),
    });
  }

  badge(statut: string): string {
    const s = statut === 'VALIDEE' ? 'valide' : statut === 'REJETEE' ? 'rejete' : 'attente';
    return `badge badge-${s}`;
  }

  compteLabel(id?: number | null): string {
    if (!id) return '—';
    const c = this.comptes().find((x) => x.id === id);
    return c ? c.numeroCompte : `#${id}`;
  }

  ligneRejetee(t: Transaction): boolean {
    return t.statut === 'REJETEE';
  }

  depotInvalide(champ: 'compteId' | 'montant' | 'devise'): boolean {
    return this.invalide(this.depotForm.controls[champ]);
  }

  retraitInvalide(champ: 'compteId' | 'montant' | 'devise'): boolean {
    return this.invalide(this.retraitForm.controls[champ]);
  }

  transfertInvalide(champ: 'compteSourceId' | 'compteDestId' | 'montant' | 'devise' | 'motif'): boolean {
    return this.invalide(this.transfertForm.controls[champ]);
  }

  transfertMemeCompte(): boolean {
    return this.transfertForm.hasError('sameAccount') &&
           (this.transfertForm.dirty || this.transfertForm.touched);
  }

  private apres(msg: string): void {
    this.succes.set(msg);
    this.erreur.set(null);
    this.operationEnCours.set(false);
    this.chargerComptes();
    if (this.compteHisto) this.chargerHistorique();
  }

  private echec(e: Error): void {
    this.succes.set(null);
    this.operationEnCours.set(false);
    this.chargementHistorique.set(false);
    this.erreur.set(e?.message || 'Opération refusée (vérifiez le solde ou les comptes).');
  }

  private reset(): void {
    this.erreur.set(null);
    this.succes.set(null);
    this.transactionCreee.set(null);
  }

  private invalide(control: AbstractControl): boolean {
    return control.invalid && (control.dirty || control.touched);
  }
}

// Validateur de groupe : source ≠ destination
function comptesDifferents(control: AbstractControl): ValidationErrors | null {
  const src = control.get('compteSourceId')?.value;
  const dst = control.get('compteDestId')?.value;
  return src && dst && src === dst ? { sameAccount: true } : null;
}
