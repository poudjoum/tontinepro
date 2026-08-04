import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError } from 'rxjs';
import { tap, finalize, shareReplay } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  AuthResponse, LoginRequest, LoginResponse,
  RegisterRequest, TwoFaSetupResponse, isTwoFaChallenge
} from '../models/auth.model';

const TOKEN_KEY = 'tp_auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private api = `${environment.apiUrl}/auth`;

  private _auth = signal<AuthResponse | null>(this.loadFromStorage());

  /** Rafraîchissement partagé tant qu'il est en vol (voir {@link rafraichir}). */
  private refreshEnCours: Observable<AuthResponse> | null = null;

  readonly isLoggedIn    = computed(() => !!this._auth()?.accessToken);
  readonly currentUser   = computed(() => this._auth());
  readonly isSuperAdmin  = computed(() => this._auth()?.role === 'SUPER_ADMIN');
  readonly isAdmin       = computed(() => this._auth()?.role === 'ADMIN');
  readonly isSecretaire  = computed(() => this._auth()?.role === 'SECRETAIRE');
  readonly isGestionnaire = computed(() =>
    this._auth()?.role === 'ADMIN' || this._auth()?.role === 'SECRETAIRE');
  readonly isMembre      = computed(() => this._auth()?.role === 'MEMBRE');
  readonly twoFaEnabled  = computed(() => this._auth()?.twoFaEnabled ?? false);

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

  /**
   * Rafraîchit le jeton d'accès en garantissant un seul appel en vol.
   *
   * Le refresh token est à usage unique : le serveur le révoque dès qu'il sert.
   * Deux appels concurrents portant le même jeton se sabotent donc — le premier
   * réussit et révoque, le second reçoit « Refresh token révoqué » et fait
   * déconnecter l'utilisateur. Or c'est le cas courant : quand un écran charge
   * plusieurs ressources en parallèle, toutes tombent en 401 ensemble à
   * l'expiration. Les appelants simultanés partagent donc la même requête.
   */
  rafraichir(): Observable<AuthResponse> {
    if (this.refreshEnCours) return this.refreshEnCours;

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) return throwError(() => new Error('Aucun refresh token disponible'));

    this.refreshEnCours = this.http.post<AuthResponse>(`${this.api}/refresh`, { refreshToken }).pipe(
      tap(auth => this.saveAuth(auth)),
      // Avant shareReplay : ne se déclenche qu'une fois, à la fin de la requête
      // partagée, et non à chaque désabonnement d'appelant.
      finalize(() => { this.refreshEnCours = null; }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    return this.refreshEnCours;
  }

  changerMotDePasse(nouveauMotDePasse: string) {
    return this.http.post<AuthResponse>(`${this.api}/changer-mot-de-passe`, { nouveauMotDePasse }).pipe(
      tap(auth => this.saveAuth(auth))
    );
  }

  readonly mustChangePassword = computed(() => this._auth()?.mustChangePassword === true);

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

  // ── 2FA profile management ─────────────────────────────────────────

  setup2fa() {
    return this.http.post<TwoFaSetupResponse>(`${this.api}/2fa/activer`, {});
  }

  confirm2fa(code: string) {
    return this.http.post<void>(`${this.api}/2fa/confirmer`, { code }).pipe(
      tap(() => {
        const current = this._auth();
        if (current) this.saveAuth({ ...current, twoFaEnabled: true });
      })
    );
  }

  disable2fa(code: string) {
    return this.http.post<void>(`${this.api}/2fa/desactiver`, { code }).pipe(
      tap(() => {
        const current = this._auth();
        if (current) this.saveAuth({ ...current, twoFaEnabled: false });
      })
    );
  }

  private clearAuth(): void {
    localStorage.removeItem(TOKEN_KEY);
    this._auth.set(null);
    // Sans cette remise à zéro, un rafraîchissement encore en vol resterait
    // partagé après la déconnexion et servirait un jeton au compte suivant.
    this.refreshEnCours = null;
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
