import { JwtPayload } from '../models/auth.models';

/**
 * Décode le payload d'un JWT en base64 — sans librairie externe.
 * Ne valide PAS la signature (c'est le rôle du serveur).
 */
export function decodeJwt(token: string): JwtPayload | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    // Padding base64url → base64 standard
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded  = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
    const json    = decodeURIComponent(
      atob(padded)
        .split('')
        .map(c => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join('')
    );
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

/** true si le token existe et n'est pas expiré */
export function isTokenValid(token: string | null): boolean {
  if (!token) return false;
  const payload = decodeJwt(token);
  if (!payload) return false;
  return Date.now() / 1000 < payload.exp;
}

/** Secondes restantes avant expiration (0 si déjà expiré) */
export function secondsUntilExpiry(token: string): number {
  const payload = decodeJwt(token);
  if (!payload) return 0;
  return Math.max(0, payload.exp - Math.floor(Date.now() / 1000));
}
