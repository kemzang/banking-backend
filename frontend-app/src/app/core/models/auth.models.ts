// Contrats correspondant a auth-service (cf. docs/contracts/01-api-rest.md)
export interface RegisterRequest {
  email: string;
  motDePasse: string;
  telephone?: string;
}

export interface LoginRequest {
  email: string;
  motDePasse: string;
}

export interface AuthResponse {
  token: string;
  type: string;        // "Bearer"
  expiresIn: number;   // secondes
}

export interface UserResponse {
  id: string;
  email: string;
  roles: string[];
}
