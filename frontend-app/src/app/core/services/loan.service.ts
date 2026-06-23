import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DemandePret {
  id: number;
  clientId: number;
  montantDemande: number;
  dureeMois: number;
  scoreRisque: number;
  statut: string;
}

export interface Pret {
  id: number;
  clientId: number;
  montantAccorde: number;
  tauxInteret: number;
  dureeMois: number;
  capitalRestant: number;
  statut: string;
}

export interface Echeance {
  numero: number;
  dateEcheance: string;
  montantCapital: number;
  montantInteret: number;
  montantTotal: number;
  statut: string;
}

export interface Echeancier {
  pretId: number;
  echeances: Echeance[];
}

export interface ClientLoanRequest {
  montantDemande: number;
  dureeMois: number;
  motif: string;
}

@Injectable({ providedIn: 'root' })
export class LoanService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/loans`;

  soumettre(req: { clientId: number; montantDemande: number; dureeMois: number; motif: string }): Observable<DemandePret> {
    return this.http.post<DemandePret>(`${this.base}/applications`, req);
  }
  soumettreMaDemande(req: ClientLoanRequest): Observable<DemandePret> {
    return this.http.post<DemandePret>(`${this.base}/applications/mine`, req);
  }
  mesDemandes(): Observable<DemandePret[]> {
    return this.http.get<DemandePret[]>(`${this.base}/applications/mine`);
  }
  mesPrets(): Observable<Pret[]> {
    return this.http.get<Pret[]>(`${this.base}/mine`);
  }
  getDemande(id: number): Observable<DemandePret> {
    return this.http.get<DemandePret>(`${this.base}/applications/${id}`);
  }
  decider(id: number, decision: { approuver: boolean; tauxInteret: number | null; compteId: number | null; motifRejet: string | null }): Observable<Pret> {
    return this.http.post<Pret>(`${this.base}/applications/${id}/decision`, decision);
  }
  getPret(id: number): Observable<Pret> {
    return this.http.get<Pret>(`${this.base}/${id}`);
  }
  getEcheancier(id: number): Observable<Echeancier> {
    return this.http.get<Echeancier>(`${this.base}/${id}/schedule`);
  }
  rembourser(id: number, req: { montant: number; moyenPaiement: string }): Observable<Pret> {
    return this.http.post<Pret>(`${this.base}/${id}/repay`, req);
  }
}
