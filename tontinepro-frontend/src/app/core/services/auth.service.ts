import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  AuthResponse, LoginRequest, LoginResponse,
  RegisterRequest, isTwoFaChallenge
} from '../models/auth.model';

const TOKEN_KEY = 'tp_auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = `${environment.apiUrl}/auth`;

  private _auth = signal<AuthResponse | null>(this.loadFromStorage());

  readonly isLoggedIn = computed(() => !!this._auth()?.accessToken);
  readonly currentUser = computed(() => this._auth());
  readonly isAdmin = computed(() => this._auth()?.role === 'ADMIN');
  readonly isMembre = computed(() => this._auth()?.role === 'MEMBRE');

  constructor(private http: HttpClient, private router: Router) {}

  login(req: LoginRequest) {
    return this.http.post<LoginResponse>(`${this.api}/login`, req);
  }

  validate2fa(ticket: string, code: string) {
    return this.http.post<AuthResponse>(`${this.api}/2fa/valider`, { ticket, code }).pipe(
      tap(auth => this.saveAuth(auth))
    );
  }

  register(req: RegisterRequest) {
    return this.http.post<AuthResponse>(`${this.api}/register`, req).pipe(
      tap(auth => this.saveAuth(auth))
    );
  }

  refresh(refreshToken: string) {
    return this.http.post<AuthResponse>(`${this.api}/refresh`, { refreshToken }).pipe(
      tap(auth => this.saveAuth(auth))
    );
  }

  logout(): void {
    this.http.post(`${this.api}/logout`, {}).subscribe({ error: () => {} });
    this.clearAuth();
    this.router.navigate(['/auth/login']);
  }

  saveAuth(auth: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, JSON.stringify(auth));
    this._auth.set(auth);
  }

  getAccessToken(): string | null {
    return this._auth()?.accessToken ?? null;
  }

  getRefreshToken(): string | null {
    return this._auth()?.refreshToken ?? null;
  }

  private clearAuth(): void {
    localStorage.removeItem(TOKEN_KEY);
    this._auth.set(null);
  }

  private loadFromStorage(): AuthResponse | null {
    try {
      const raw = localStorage.getItem(TOKEN_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
