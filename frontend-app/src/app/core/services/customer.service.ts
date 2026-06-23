import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Operateur {
  id: number;
  nom: string;
  type: string;
  code: string;
  statut?: string;
}

export interface Adresse {
  rue: string;
  ville: string;
  pays: string;
  codePostal: string;
}

export interface Client {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  statutKyc: string;
  operateurId: number;
  motifRejet?: string | null;
}

export interface ClientRequest {
  operateurId: number;
  nom: string;
  prenom: string;
  dateNaissance: string;
  email: string;
  telephone: string;
  numeroIdentite: string;
  typePiece: string;
  adresse: Adresse;
}

@Injectable({ providedIn: 'root' })
export class CustomerService {
  private http = inject(HttpClient);
  private opUrl = `${environment.apiUrl}/operators`;
  private clUrl = `${environment.apiUrl}/customers`;

  // ----- Operateurs -----
  getOperateurs(): Observable<Operateur[]> {
    return this.http.get<Operateur[]>(this.opUrl);
  }
  createOperateur(op: { nom: string; type: string; code: string; statut?: string }): Observable<Operateur> {
    return this.http.post<Operateur>(this.opUrl, op);
  }
  getOperateursActifs(): Observable<Operateur[]> {
    return this.http.get<Operateur[]>(`${this.opUrl}/active`);
  }
  updateOperateurStatus(id: number, status: string): Observable<Operateur> {
    return this.http.patch<Operateur>(`${this.opUrl}/${id}/status`, { status });
  }

  // ----- Clients -----
  getClients(): Observable<Client[]> {
    return this.http.get<Client[]>(this.clUrl);
  }
  getClient(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.clUrl}/${id}`);
  }
  getClientParEmail(email: string): Observable<Client> {
    return this.http.get<Client>(`${this.clUrl}/by-email/${email}`);
  }
  createClient(req: ClientRequest): Observable<Client> {
    return this.http.post<Client>(this.clUrl, req);
  }
  majKyc(id: number, statutKyc: string): Observable<Client> {
    return this.http.patch<Client>(`${this.clUrl}/${id}/kyc`, { statutKyc });
  }
  getClientsEnAttente(): Observable<Client[]> {
    return this.http.get<Client[]>(`${this.clUrl}/pending`);
  }
  approuverClient(id: number): Observable<Client> {
    return this.http.patch<Client>(`${this.clUrl}/${id}/approve`, {});
  }
  rejeterClient(id: number, reason: string): Observable<Client> {
    return this.http.patch<Client>(`${this.clUrl}/${id}/reject`, { reason });
  }
}
