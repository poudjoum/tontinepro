export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  telephone?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  email: string;
  role: 'ADMIN' | 'SECRETAIRE' | 'MEMBRE' | 'INVITE';
  twoFaEnabled: boolean;
}

export interface TwoFaSetupResponse {
  secret: string;
  qrCodeDataUri: string;
}

export interface TwoFaChallengeResponse {
  requires2fa: true;
  ticket: string;
  email: string;
}

export type LoginResponse = AuthResponse | TwoFaChallengeResponse;

export function isTwoFaChallenge(r: LoginResponse): r is TwoFaChallengeResponse {
  return (r as TwoFaChallengeResponse).requires2fa === true;
}
