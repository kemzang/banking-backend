import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

// Correspond a OcrAnalysisResponse (ai-document-service)
export interface OcrAnalysis {
  id: number;
  original_filename: string;
  extracted_text: string | null;
  confidence_score: number;
  status: string;
  created_at: string;
}

// Enveloppe { status, message, data } renvoyee par le service Python
interface ApiEnvelope<T> {
  status: string;
  message: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/v1/ocr`;

  // Envoie une image et recupere le texte extrait
  extract(file: File): Observable<OcrAnalysis> {
    const form = new FormData();
    form.append('file', file);
    return this.http
      .post<ApiEnvelope<OcrAnalysis>>(`${this.base}/extract`, form)
      .pipe(map((res) => res.data));
  }

  // Historique des analyses
  history(): Observable<OcrAnalysis[]> {
    return this.http
      .get<ApiEnvelope<OcrAnalysis[]>>(`${this.base}/history`)
      .pipe(map((res) => res.data));
  }
}
