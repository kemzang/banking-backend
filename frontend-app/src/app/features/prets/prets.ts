import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { DemandePret, Echeancier, LoanService, Pret } from '../../core/services/loan.service';
import { Client, CustomerService } from '../../core/services/customer.service';
import { AccountService, Compte } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { DocumentService, OcrAnalysis } from '../../core/services/document.service';

@Component({
  selector: 'app-prets',
  imports: [FormsModule, DecimalPipe, DatePipe],
  templateUrl: './prets.html',
})
export class Prets implements OnInit {
  private loan = inject(LoanService);
  private customer = inject(CustomerService);
  private account = inject(AccountService);
  private auth = inject(AuthService);
  private docService = inject(DocumentService);

  clients = signal<Client[]>([]);
  comptes = signal<Compte[]>([]);
  demandes = signal<DemandePret[]>([]);
  prets = signal<Pret[]>([]);
  mesPrets = signal<Pret[]>([]);
  echeancier = signal<Echeancier | null>(null);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);
  documentsClient = signal<OcrAnalysis[]>([]);

  nouvelle = { clientId: 0, montantDemande: 0, dureeMois: 12, motif: '' };
  tauxInteret = 0.12;
  compteDecision = 0;
  remb = { montant: 0, moyenPaiement: 'COMPTE' };

  ngOnInit(): void {
    if (this.estClient()) {
      this.chargerPourClient();
    } else {
      this.loan.getDemandes().subscribe({ next: (d) => this.demandes.set(d) });
      this.loan.getPrets().subscribe({ next: (p) => this.prets.set(p) });
      this.customer.getClients().subscribe({ next: (c) => this.clients.set(c) });
      this.account.list().subscribe({ next: (a) => this.comptes.set(a) });
    }
  }

  estClient(): boolean {
    const estAdminOuOperateur = this.auth.hasRole('ADMIN') || this.auth.hasRole('OPERATEUR');
    return !estAdminOuOperateur && this.auth.hasRole('CLIENT');
  }

  private chargerPourClient(): void {
    const email = this.auth.email();
    if (!email) return;
    this.customer.getClientParEmail(email).subscribe({
      next: (client) => {
        this.nouvelle.clientId = client.id;
        this.loan.getMesPrets(client.id).subscribe({ next: (p) => this.mesPrets.set(p) });
        this.loan.getMesDemandes(client.id).subscribe({ next: (d) => this.demandes.set(d) });
        this.chargerDocuments(client.id);
      },
    });
  }

  chargerDocuments(clientId: number): void {
    this.docService.getByClient(clientId).subscribe({
      next: (docs) => this.documentsClient.set(docs),
      error: () => this.documentsClient.set([]),
    });
  }

  onClientChange(clientId: number): void {
    if (clientId > 0) {
      this.chargerDocuments(clientId);
    } else {
      this.documentsClient.set([]);
    }
  }

  soumettre(): void {
    this.reset();
    this.loan.soumettre(this.nouvelle).subscribe({
      next: (d) => {
        this.demandes.update((list) => [d, ...list]);
        this.succes.set(`Demande #${d.id} soumise (score risque ${d.scoreRisque}).`);
        this.nouvelle = { clientId: this.estClient() ? this.nouvelle.clientId : 0, montantDemande: 0, dureeMois: 12, motif: '' };
      },
      error: () => this.erreur.set('Erreur lors de la soumission.'),
    });
  }

  approuver(d: DemandePret): void {
    this.reset();
    this.loan.decider(d.id, { approuver: true, tauxInteret: this.tauxInteret, compteId: this.compteDecision, motifRejet: null }).subscribe({
      next: (p) => {
        this.prets.update((list) => [p, ...list]);
        this.majStatut(d.id, 'APPROUVEE');
        this.succes.set(`Prêt #${p.id} créé pour la demande #${d.id}.`);
      },
      error: (e) => this.erreur.set(e?.error?.message || 'Erreur lors de la décision (choisissez un compte de versement).'),
    });
  }

  rejeter(d: DemandePret): void {
    this.reset();
    this.loan.decider(d.id, { approuver: false, tauxInteret: null, compteId: null, motifRejet: 'Non éligible' }).subscribe({
      next: () => { this.majStatut(d.id, 'REJETEE'); this.succes.set(`Demande #${d.id} rejetée.`); },
      error: () => this.erreur.set('Erreur lors du rejet.'),
    });
  }

  voirEcheancier(p: Pret): void {
    this.loan.getEcheancier(p.id).subscribe({ next: (e) => this.echeancier.set(e), error: () => this.erreur.set('Erreur échéancier.') });
  }

  rembourser(p: Pret): void {
    this.reset();
    this.loan.rembourser(p.id, this.remb).subscribe({
      next: (maj) => { this.remplacerPret(maj); this.succes.set(`Remboursement enregistré (prêt #${p.id}).`); this.voirEcheancier(maj); },
      error: (e) => this.erreur.set(e?.error?.message || 'Erreur lors du remboursement.'),
    });
  }

  nomClient(id: number): string {
    const c = this.clients().find((x) => x.id === id);
    return c ? `${c.prenom} ${c.nom}` : `#${id}`;
  }

  labelTypeDoc(type: string | null): string {
    const labels: Record<string, string> = {
      salaire: 'Bulletin de salaire',
      releve_bancaire: 'Relevé bancaire',
      cni: 'Pièce d\'identité',
      inconnu: 'Non identifié',
    };
    return type ? (labels[type] || type) : 'Non identifié';
  }

  objectKeys(obj: Record<string, unknown> | null): string[] {
    return obj ? Object.keys(obj) : [];
  }

  badgeD(s: string): string {
    const k = s === 'APPROUVEE' ? 'valide' : s === 'REJETEE' ? 'rejete' : 'attente';
    return `badge badge-${k}`;
  }
  badgeE(s: string): string {
    const k = s === 'PAYEE' ? 'valide' : s === 'EN_RETARD' ? 'rejete' : 'attente';
    return `badge badge-${k}`;
  }

  private majStatut(id: number, statut: string): void {
    this.demandes.update((list) => list.map((d) => (d.id === id ? { ...d, statut } : d)));
  }
  private remplacerPret(p: Pret): void {
    this.prets.update((list) => list.map((x) => (x.id === p.id ? p : x)));
  }
  private reset(): void { this.erreur.set(null); this.succes.set(null); }
}
