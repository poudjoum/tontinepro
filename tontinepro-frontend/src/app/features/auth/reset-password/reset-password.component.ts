import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-reset-password',
  imports: [FormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
})
export class ResetPasswordComponent implements OnInit {
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  private http   = inject(HttpClient);

  private api = `${environment.apiUrl}/auth/reset-password`;

  token        = '';
  tokenValide  = signal<boolean | null>(null); // null = en cours de vérif
  nouveauMdp   = signal('');
  confirmation = signal('');
  submitting   = signal(false);
  success      = signal(false);
  error        = signal('');

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.tokenValide.set(false);
      return;
    }
    this.http.get<{ valide: boolean }>(`${this.api}/verify`, { params: { token: this.token } })
      .subscribe({
        next:  r => this.tokenValide.set(r.valide),
        error: () => this.tokenValide.set(false),
      });
  }

  soumettre(): void {
    const mdp = this.nouveauMdp();
    if (mdp.length < 8) { this.error.set('Le mot de passe doit faire au moins 8 caractères.'); return; }
    if (mdp !== this.confirmation()) { this.error.set('Les mots de passe ne correspondent pas.'); return; }
    this.submitting.set(true);
    this.error.set('');
    this.http.post<void>(`${this.api}/confirmer`, { token: this.token, nouveauMotDePasse: mdp })
      .subscribe({
        next: () => { this.success.set(true); this.submitting.set(false); },
        error: e => {
          this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de la réinitialisation.');
          this.submitting.set(false);
        },
      });
  }
}
