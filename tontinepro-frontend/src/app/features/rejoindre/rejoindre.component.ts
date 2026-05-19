import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InvitationService } from '../../core/services/invitation.service';
import { AuthService } from '../../core/services/auth.service';
import { StatutInvitationResponse } from '../../core/models/invitation.model';

@Component({
  selector: 'app-rejoindre',
  imports: [FormsModule, RouterLink],
  templateUrl: './rejoindre.component.html',
})
export class RejoindreComponent implements OnInit {
  private route  = inject(ActivatedRoute);
  private router = inject(Router);
  private svc    = inject(InvitationService);
  private auth   = inject(AuthService);

  token   = '';
  statut  = signal<StatutInvitationResponse | null>(null);
  loading = signal(true);
  joining = signal(false);
  error   = signal('');

  form = {
    email: '',
    password: '',
    confirmPassword: '',
    nom: '',
    prenom: '',
    telephone: '',
  };

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') ?? '';
    if (!this.token) { this.router.navigate(['/']); return; }

    this.svc.statut(this.token).subscribe({
      next: s => { this.statut.set(s); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }

  rejoindre(): void {
    if (this.form.password !== this.form.confirmPassword) {
      this.error.set('Les mots de passe ne correspondent pas');
      return;
    }
    this.joining.set(true);
    this.error.set('');

    this.svc.rejoindre(this.token, {
      email: this.form.email,
      password: this.form.password,
      nom: this.form.nom,
      prenom: this.form.prenom,
      telephone: this.form.telephone || undefined,
    }).subscribe({
      next: auth => {
        this.auth.saveAuth(auth);
        this.router.navigate(['/dashboard']);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors de l\'adhésion');
        this.joining.set(false);
      },
    });
  }
}
