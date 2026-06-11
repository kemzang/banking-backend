import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Compte {
  id: number;
  numeroCompte: string;
  clientId: number;
  operateurId: number;
  type: string;
  solde: number;
  devise: string;
  plafondJournalier: number | null;
  decouvertAutorise: number;
  statut: string;
  dateOuverture: string;
}

export interface CompteRequest {
  clientId: number;
  operateurId: number;
  type: string;
  devise: string;
  plafondJournalier: number | null;
  decouvertAutorise: number;
}

@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/accounts`;

  list(clientId?: number): Observable<Compte[]> {
    const url = clientId ? `${this.base}?clientId=${clientId}` : this.base;
    return this.http.get<Compte[]>(url);
  }
  getById(id: number): Observable<Compte> {
    return this.http.get<Compte>(`${this.base}/${id}`);
  }
  open(req: CompteRequest): Observable<Compte> {
    return this.http.post<Compte>(this.base, req);
  }
  suspend(id: number): Observable<Compte> {
    return this.http.patch<Compte>(`${this.base}/${id}/suspend`, {});
  }
  close(id: number): Observable<Compte> {
    return this.http.patch<Compte>(`${this.base}/${id}/close`, {});
  }
}
