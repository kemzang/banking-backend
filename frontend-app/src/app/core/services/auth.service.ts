import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import {
  AuthResponse, ForgotPasswordRequest, JwtPayload,
  LoginRequest, MfaBackupRequest, MfaResendRequest,
  MfaSetupResponse, MfaVerifyRequest, MfaVerifyResponse,
  RefreshResponse, RegisterRequest, ResetPasswordRequest, UserResponse,
} from '../models/auth.models';
import { decodeJwt, isTokenValid } from '../utils/jwt.utils';

const TOKEN_KEY = 'bank_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http   = inject(HttpClient);
  private router = inject(Router);
  private base   = `${environment.apiUrl}/auth`;

  /** Signal réactif — consommé par la UI et les guards */
  readonly connecte = signal<boolean>(this.hasValidToken());

  // ── Token helpers ─────────────────────────────────────

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private storeToken(t: string): void {
    localStorage.setItem(TOKEN_KEY, t);
    this.connecte.set(true);
  }

  private clearToken(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.connecte.set(false);
  }

  hasValidToken(): boolean {
    return isTokenValid(this.token);
  }

  payload(): JwtPayload | null {
    return decodeJwt(this.token ?? '');
  }

  email(): string      { return this.payload()?.sub ?? ''; }
  roles(): string[]    { return this.payload()?.roles ?? []; }
  mfaEnabled(): boolean  { return this.payload()?.mfaEnabled ?? false; }
  mfaVerified(): boolean { return this.payload()?.mfaVerified ?? false; }

  hasRole(...roles: string[]): boolean {
    const mine = this.roles();
    return roles.some(r => mine.includes(r));
  }

  /** Route cible selon le rôle principal */
  redirectByRole(): string {
    if (this.hasRole('ADMIN'))     return '/dashboard';
    if (this.hasRole('OPERATEUR')) return '/dashboard';
    return '/dashboard';
  }

  // ── Authentification ──────────────────────────────────

  register(req: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.base}/register`, req);
  }

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, req).pipe(
      tap(res => this.storeToken(res.token)),
    );
  }

  googleLogin(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/google`, { idToken }).pipe(
      tap(res => this.storeToken(res.token)),
    );
  }

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.base}/me`);
  }

  logout(): void {
    // fire-and-forget — on ne bloque pas la déconnexion sur la réponse serveur
    this.http.post(`${this.base}/logout`, {}).subscribe({ error: () => {} });
    this.clearToken();
    window.location.href = '/login';
  }

  refresh(): Observable<RefreshResponse> {
    return this.http.post<RefreshResponse>(`${this.base}/refresh`, { token: this.token }).pipe(
      tap(res => this.storeToken(res.token)),
    );
  }

  // ── MFA ───────────────────────────────────────────────

  mfaVerify(req: MfaVerifyRequest): Observable<MfaVerifyResponse> {
    return this.http.post<MfaVerifyResponse>(`${this.base}/mfa/verify`, req).pipe(
      tap(res => this.storeToken(res.token)),
    );
  }

  mfaVerifyBackup(req: MfaBackupRequest): Observable<MfaVerifyResponse> {
    return this.http.post<MfaVerifyResponse>(`${this.base}/mfa/verify-backup`, req).pipe(
      tap(res => this.storeToken(res.token)),
    );
  }

  mfaResend(req: MfaResendRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/mfa/resend`, req);
  }

  mfaSetup(): Observable<MfaSetupResponse> {
    return this.http.get<MfaSetupResponse>(`${this.base}/mfa/setup`);
  }

  mfaSetupVerify(code: string): Observable<void> {
    return this.http.post<void>(`${this.base}/mfa/setup/verify`, { code });
  }

  mfaDisable(code: string): Observable<void> {
    return this.http.post<void>(`${this.base}/mfa/disable`, { code });
  }

  // ── Mot de passe oublié ───────────────────────────────

  forgotPassword(req: ForgotPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/forgot-password`, req);
  }

  resetPassword(req: ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/reset-password`, req);
  }
}
