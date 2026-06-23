import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocumentService, OcrAnalysis } from '../../core/services/document.service';
import { Client, CustomerService } from '../../core/services/customer.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-documents',
  imports: [DecimalPipe, FormsModule],
  templateUrl: './documents.html',
  styleUrl: './documents.scss',
})
export class Documents implements OnInit {
  private docService = inject(DocumentService);
  private customer = inject(CustomerService);
  private auth = inject(AuthService);

  fichier: File | null = null;
  apercu = signal<string | null>(null);
  resultat = signal<OcrAnalysis | null>(null);
  historique = signal<OcrAnalysis[]>([]);
  chargement = signal(false);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);

  // rattachement au processus metier (KYC)
  clients = signal<Client[]>([]);
  clientId = 0;
  typeDoc = 'CNI';

  ngOnInit(): void {
    if (!this.estClient()) this.chargerHistorique();
    this.customer.getClients().subscribe({ next: (c) => this.clients.set(c), error: () => {} });
  }

  onFichier(e: Event): void {
    const input = e.target as HTMLInputElement;
    this.fichier = input.files?.[0] ?? null;
    this.resultat.set(null);
    this.apercu.set(this.fichier ? URL.createObjectURL(this.fichier) : null);
  }

  analyser(): void {
    if (!this.fichier) return;
    this.erreur.set(null);
    this.succes.set(null);
    this.chargement.set(true);
    this.docService.extract(this.fichier).subscribe({
      next: (res) => {
        this.resultat.set(res);
        this.historique.update(items => [res, ...items]);
        this.chargement.set(false);
        if (!this.estClient()) this.chargerHistorique();
      },
      error: () => {
        this.erreur.set("Erreur lors de l'analyse du document.");
        this.chargement.set(false);
      },
    });
  }

  // Alimente le processus KYC : valide l'identite du client rattache au document analyse.
  validerKyc(): void {
    if (!this.clientId) {
      this.erreur.set('Sélectionnez un client à rattacher au document.');
      return;
    }
    this.customer.majKyc(this.clientId, 'VALIDE').subscribe({
      next: (c) => this.succes.set(`KYC validé pour ${c.prenom} ${c.nom} (document ${typeDocLabel(this.typeDoc)} vérifié).`),
      error: () => this.erreur.set('Erreur lors de la validation du KYC.'),
    });
  }

  chargerHistorique(): void {
    this.docService.history().subscribe({ next: (h) => this.historique.set(h) });
  }

  estClient(): boolean { return this.auth.hasRole('CLIENT'); }
}

function typeDocLabel(t: string): string {
  return t;
}
