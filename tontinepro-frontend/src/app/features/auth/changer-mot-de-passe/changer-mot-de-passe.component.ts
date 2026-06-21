import { Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { PostAuthNavService } from '../../../core/services/post-auth-nav.service';

@Component({
  selector: 'app-changer-mot-de-passe',
  imports: [FormsModule],
  templateUrl: './changer-mot-de-passe.component.html',
})
export class ChangerMotDePasseComponent {
  private auth    = inject(AuthService);
  private postNav = inject(PostAuthNavService);

  nouveauMdp   = signal('');
  confirmation = signal('');
  submitting   = signal(false);
  error        = signal('');

  soumettre(): void {
    const mdp = this.nouveauMdp();
    if (mdp.length < 8) { this.error.set('Le mot de passe doit faire au moins 8 caractères.'); return; }
    if (mdp !== this.confirmation()) { this.error.set('Les mots de passe ne correspondent pas.'); return; }

    this.submitting.set(true);
    this.error.set('');
    this.auth.changerMotDePasse(mdp).subscribe({
      next: () => {
        this.submitting.set(false);
        this.postNav.navigateAfterLogin();
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors du changement de mot de passe.');
        this.submitting.set(false);
      },
    });
  }

  deconnexion(): void {
    this.auth.logout();
  }
}
