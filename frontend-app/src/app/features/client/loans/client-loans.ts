import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { LoanService, DemandePret, Pret } from '../../../core/services/loan.service';

@Component({
  selector: 'app-client-loans',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './client-loans.html',
  styleUrl: './client-loans.scss',
})
export class ClientLoans implements OnInit {
  private loans = inject(LoanService);
  demandes = signal<DemandePret[]>([]);
  prets = signal<Pret[]>([]);
  chargement = signal(true);
  erreur = signal<string | null>(null);
  succes = signal<string | null>(null);
  form = { montantDemande: 0, dureeMois: 12, motif: '' };

  ngOnInit(): void { this.charger(); }

  charger(): void {
    this.chargement.set(true);
    this.loans.mesDemandes().subscribe({
      next: demandes => { this.demandes.set(demandes); this.chargement.set(false); },
      error: () => { this.erreur.set('Impossible de charger vos demandes.'); this.chargement.set(false); },
    });
    this.loans.mesPrets().subscribe({ next: prets => this.prets.set(prets) });
  }

  soumettre(): void {
    this.erreur.set(null);
    this.succes.set(null);
    this.loans.soumettreMaDemande(this.form).subscribe({
      next: demande => {
        this.demandes.update(items => [demande, ...items]);
        this.form = { montantDemande: 0, dureeMois: 12, motif: '' };
        this.succes.set(`Votre demande #${demande.id} a été enregistrée.`);
      },
      error: () => this.erreur.set('Votre demande n’a pas pu être enregistrée.'),
    });
  }

  badge(status: string): string {
    if (status.includes('REJET')) return 'badge badge-rejete';
    if (status.includes('APPROUV') || status === 'ACTIF') return 'badge badge-valide';
    return 'badge badge-attente';
  }
}
