import { Component, OnInit, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { DocumentService, OcrAnalysis } from '../../core/services/document.service';

@Component({
  selector: 'app-documents',
  imports: [DecimalPipe],
  templateUrl: './documents.html',
  styleUrl: './documents.scss',
})
export class Documents implements OnInit {
  private docService = inject(DocumentService);

  fichier: File | null = null;
  apercu = signal<string | null>(null);
  resultat = signal<OcrAnalysis | null>(null);
  historique = signal<OcrAnalysis[]>([]);
  chargement = signal(false);
  erreur = signal<string | null>(null);

  ngOnInit(): void {
    this.chargerHistorique();
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
    this.chargement.set(true);
    this.docService.extract(this.fichier).subscribe({
      next: (res) => {
        this.resultat.set(res);
        this.chargement.set(false);
        this.chargerHistorique();
      },
      error: () => {
        this.erreur.set("Erreur lors de l'analyse du document.");
        this.chargement.set(false);
      },
    });
  }

  chargerHistorique(): void {
    this.docService.history().subscribe({ next: (h) => this.historique.set(h) });
  }
}
