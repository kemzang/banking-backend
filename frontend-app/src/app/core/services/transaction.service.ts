import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TransactionType = 'DEPOT' | 'RETRAIT' | 'TRANSFERT';

export type TransactionStatus = 'INITIEE' | 'VALIDEE' | 'REJETEE';

export interface DepositRequest {
  compteId: number;
  montant: number;
  devise: string;
}

export interface WithdrawRequest {
  compteId: number;
  montant: number;
  devise: string;
}

export interface TransferRequest {
  compteSourceId: number;
  compteDestId: number;
  montant: number;
  devise: string;
  motif?: string;
}

export interface Transaction {
  id: number;
  reference: string;
  type: TransactionType;
  montant: number;
  devise: string;
  compteSourceId?: number | null;
  compteDestId?: number | null;
  commission: number;
  statut: TransactionStatus;
  motif?: string | null;
  dateOperation: string;
}

export interface TransactionErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  deposit(req: DepositRequest): Observable<Transaction> {
    return this.http
      .post<Transaction>(`${this.apiUrl}/transactions/deposit`, req)
      .pipe(catchError((error) => this.handleError(error)));
  }

  withdraw(req: WithdrawRequest): Observable<Transaction> {
    return this.http
      .post<Transaction>(`${this.apiUrl}/transactions/withdraw`, req)
      .pipe(catchError((error) => this.handleError(error)));
  }

  transfer(req: TransferRequest): Observable<Transaction> {
    return this.http
      .post<Transaction>(`${this.apiUrl}/transactions/transfer`, req)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getTransactionById(id: number): Observable<Transaction> {
    return this.http
      .get<Transaction>(`${this.apiUrl}/transactions/${id}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getTransactionsByAccountId(accountId: number): Observable<Transaction[]> {
    const params = new HttpParams().set('accountId', accountId);
    return this.http
      .get<Transaction[]>(`${this.apiUrl}/transactions`, { params })
      .pipe(catchError((error) => this.handleError(error)));
  }

  getById(id: number): Observable<Transaction> {
    return this.getTransactionById(id);
  }

  byAccount(accountId: number): Observable<Transaction[]> {
    return this.getTransactionsByAccountId(accountId);
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    const apiError = error.error as Partial<TransactionErrorResponse> | undefined;
    const validationMessages = apiError?.validationErrors
      ? Object.values(apiError.validationErrors).filter(Boolean)
      : [];
    const message =
      validationMessages[0] ||
      apiError?.message ||
      error.message ||
      'Impossible de traiter la transaction.';

    return throwError(() => new Error(message));
  }
}
