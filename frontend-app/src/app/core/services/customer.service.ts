import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Operateur {
  id: number;
  nom: string;
  type: string;
  code: string;
}

export interface Client {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  statutKyc: string;
  operateurId: number;
}

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private http = inject(HttpClient);

  getOperateurs(): Observable<Operateur[]> {
    return this.http.get<Operateur[]>(`${environment.apiUrl}/operators`);
  }

  createOperateur(op: { nom: string; type: string; code: string }): Observable<Operateur> {
    return this.http.post<Operateur>(`${environment.apiUrl}/operators`, op);
  }

  getClients(): Observable<Client[]> {
    return this.http.get<Client[]>(`${environment.apiUrl}/customers`);
  }
}
