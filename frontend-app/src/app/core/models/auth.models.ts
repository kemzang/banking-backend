/* =========================================================
   Auth Models — contrats auth-service
   ========================================================= */

export interface RegisterRequest {
  email: string;
  motDePasse: string;
  nom?: string;
  prenom?: string;
  telephone?: string;
}

export interface LoginRequest {
  email: string;
  motDePasse: string;
}

export interface AuthResponse {
  token: string;
  accessToken: string;
  type: string;      // "Bearer"
  tokenType: string;
  expiresIn: number; // secondes
  user: UserResponse;
}

export interface UserResponse {
  id: string;
  email: string;
  roles: string[];
  operatorId?: number | null;
  firstName?: string | null;
  lastName?: string | null;
}

/** Claims extraites du JWT (champ payload décodé en base64) */
export interface JwtPayload {
  sub: string;           // email
  roles: string[];
  userId?: string;
  operatorId?: number;
  mfaEnabled?: boolean;
  mfaVerified?: boolean;
  exp: number;           // Unix timestamp secondes
  iat: number;
}

export interface MfaVerifyRequest   { email: string; code: string; }
export interface MfaBackupRequest   { email: string; backupCode: string; }
export interface MfaResendRequest   { email: string; }
export interface MfaSetupResponse   { secret: string; qrCodeUrl: string; backupCodes: string[]; }
export interface MfaVerifyResponse  { token: string; backupCodesRemaining?: number; }
export interface ForgotPasswordRequest { email: string; }
export interface ResetPasswordRequest  { token: string; newPassword: string; }
export interface RefreshResponse       { token: string; }
