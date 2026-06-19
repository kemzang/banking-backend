import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'bank_token';

  // signal exposant l'etat de connexion (pour la UI)
  readonly connecte = signal<boolean>(this.hasToken());

  register(req: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/register`, req);
  }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, req).pipe(
      tap((res) => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        this.connecte.set(true);
      }),
    );
  }

  // Connexion via Google : envoie l'ID token Google, recoit NOTRE jeton JWT.
  googleLogin(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/google`, { idToken }).pipe(
      tap((res) => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        this.connecte.set(true);
      }),
    );
  }

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.base}/me`);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.connecte.set(false);
  }

  get token(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  // Decode les roles depuis le JWT (claim "roles").
  roles(): string[] {
    const t = this.token;
    if (!t) return [];
    try {
      const payload = JSON.parse(atob(t.split('.')[1]));
      const r = payload.roles ?? [];
      return Array.isArray(r) ? r : [r];
    } catch {
      return [];
    }
  }

  email(): string {
    const t = this.token;
    if (!t) return '';
    try {
      const payload = JSON.parse(atob(t.split('.')[1]));
      return payload.sub ?? '';
    } catch {
      return '';
    }
  }

  hasRole(...attendus: string[]): boolean {
    const r = this.roles();
    return attendus.some((a) => r.includes(a));
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }
}
