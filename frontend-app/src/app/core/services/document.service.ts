import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface OcrAnalysis {
  id: number;
  original_filename: string;
  extracted_text: string | null;
  confidence_score: number;
  status: string;
  client_id: number | null;
  document_type: string | null;
  structured_data: string | null;
  structured_fields: Record<string, unknown> | null;
  created_at: string;
}

interface ApiEnvelope<T> {
  status: string;
  message: string;
  data: T;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/v1/ocr`;

  extract(file: File, clientId?: number): Observable<OcrAnalysis> {
    const form = new FormData();
    form.append('file', file);
    if (clientId !== undefined) {
      form.append('client_id', String(clientId));
    }
    return this.http
      .post<ApiEnvelope<OcrAnalysis>>(`${this.base}/extract`, form)
      .pipe(map((res) => res.data));
  }

  history(): Observable<OcrAnalysis[]> {
    return this.http
      .get<ApiEnvelope<OcrAnalysis[]>>(`${this.base}/history`)
      .pipe(map((res) => res.data));
  }

  getByClient(clientId: number): Observable<OcrAnalysis[]> {
    return this.http
      .get<ApiEnvelope<OcrAnalysis[]>>(`${this.base}/analysis/client/${clientId}`)
      .pipe(map((res) => res.data));
  }
}
