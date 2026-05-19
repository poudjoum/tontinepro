import { Component, OnInit, signal, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { MembreService } from '../../core/services/membre.service';
import { MembreResponse } from '../../core/models/membre.model';
import { TwoFaSetupResponse } from '../../core/models/auth.model';

type Phase2fa = 'idle' | 'setup-qr' | 'setup-code' | 'disable-code';

const FONCTION_LABELS: Record<string, string> = {
  PRESIDENT: 'Président',
  SECRETAIRE: 'Secrétaire',
  TRESORIER: 'Trésorier',
  CENSEUR: 'Censeur',
  MEMBRE_ORDINAIRE: 'Membre',
};

@Component({
  selector: 'app-profil',
  templateUrl: './profil.component.html',
})
export class ProfilComponent implements OnInit {
  auth = inject(AuthService);
  private membreSvc = inject(MembreService);

  membre     = signal<MembreResponse | null>(null);
  loading    = signal(true);
  submitting = signal(false);
  error      = signal('');
  success    = signal('');

  phase2fa = signal<Phase2fa>('idle');
  setup    = signal<TwoFaSetupResponse | null>(null);
  code2fa  = signal('');

  ngOnInit(): void {
    this.membreSvc.getMonProfil().subscribe({
      next:  m => { this.membre.set(m); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  activer2fa(): void {
    this.submitting.set(true);
    this.error.set('');
    this.auth.setup2fa().subscribe({
      next: s => { this.setup.set(s); this.phase2fa.set('setup-qr'); this.submitting.set(false); },
      error: e => { this.error.set(e.message ?? 'Erreur'); this.submitting.set(false); },
    });
  }

  confirmer2fa(): void {
    if (!this.code2fa().trim() || this.submitting()) return;
    this.submitting.set(true);
    this.auth.confirm2fa(this.code2fa()).subscribe({
      next: () => {
        this.phase2fa.set('idle'); this.setup.set(null); this.code2fa.set('');
        this.success.set('2FA activée avec succès !'); this.submitting.set(false);
        setTimeout(() => this.success.set(''), 3000);
      },
      error: e => { this.error.set(e.message ?? 'Code invalide'); this.submitting.set(false); },
    });
  }

  desactiver2fa(): void {
    if (!this.code2fa().trim() || this.submitting()) return;
    this.submitting.set(true);
    this.auth.disable2fa(this.code2fa()).subscribe({
      next: () => {
        this.phase2fa.set('idle'); this.code2fa.set('');
        this.success.set('2FA désactivée.'); this.submitting.set(false);
        setTimeout(() => this.success.set(''), 3000);
      },
      error: e => { this.error.set(e.message ?? 'Code invalide'); this.submitting.set(false); },
    });
  }

  annuler2fa(): void {
    this.phase2fa.set('idle'); this.setup.set(null);
    this.code2fa.set(''); this.error.set('');
  }

  fonctionLabel(f: string): string { return FONCTION_LABELS[f] ?? f; }

  initiales(): string {
    const m = this.membre();
    return m ? (m.nom.charAt(0) + m.prenom.charAt(0)).toUpperCase() : '?';
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }
}
