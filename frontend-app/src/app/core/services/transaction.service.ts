import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Transaction {
  id: number;
  reference: string;
  type: string;
  montant: number;
  devise: string;
  compteSourceId: number | null;
  compteDestId: number | null;
  commission: number;
  statut: string;
  dateOperation: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/transactions`;

  deposit(req: { compteId: number; montant: number; devise: string }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/deposit`, req);
  }
  withdraw(req: { compteId: number; montant: number; devise: string }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/withdraw`, req);
  }
  transfer(req: { compteSourceId: number; compteDestId: number; montant: number; devise: string; motif: string }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/transfer`, req);
  }
  byAccount(accountId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.base}?accountId=${accountId}`);
  }
}
