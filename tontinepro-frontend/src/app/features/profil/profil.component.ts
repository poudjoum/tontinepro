import { Component, OnInit, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { MembreService } from '../../core/services/membre.service';
import { ProfilService } from '../../core/services/profil.service';
import { MembreResponse } from '../../core/models/membre.model';
import { TwoFaSetupResponse } from '../../core/models/auth.model';

type Phase2fa = 'idle' | 'setup-qr' | 'setup-code' | 'disable-code';
type Section  = 'infos' | 'mdp';

const FONCTION_LABELS: Record<string, string> = {
  PRESIDENT: 'Président', SECRETAIRE: 'Secrétaire', TRESORIER: 'Trésorier',
  CENSEUR: 'Censeur', MEMBRE_ORDINAIRE: 'Membre',
};

@Component({
  selector: 'app-profil',
  imports: [FormsModule],
  templateUrl: './profil.component.html',
})
export class ProfilComponent implements OnInit {
  auth = inject(AuthService);
  private membreSvc = inject(MembreService);
  private profilSvc = inject(ProfilService);

  membre     = signal<MembreResponse | null>(null);
  loading    = signal(true);
  submitting = signal(false);
  error      = signal('');
  success    = signal('');

  // ── Édition profil ─────────────────────────────────────────────────────
  editSection = signal<Section | null>(null);
  editForm = { nom: '', prenom: '', telephone: '' };

  // ── Changement mot de passe ────────────────────────────────────────────
  mdpForm  = { ancien: '', nouveau: '', confirmation: '' };
  mdpError = signal('');

  // ── 2FA ────────────────────────────────────────────────────────────────
  phase2fa = signal<Phase2fa>('idle');
  setup    = signal<TwoFaSetupResponse | null>(null);
  code2fa  = signal('');

  ngOnInit(): void {
    this.membreSvc.getMonProfil().subscribe({
      next:  m => { this.membre.set(m); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  // ── Profil ─────────────────────────────────────────────────────────────

  ouvrirEdit(): void {
    const m = this.membre();
    if (m) this.editForm = { nom: m.nom, prenom: m.prenom, telephone: '' };
    this.editSection.set('infos');
    this.error.set(''); this.success.set('');
  }

  sauvegarderProfil(): void {
    if (this.submitting()) return;
    this.submitting.set(true); this.error.set(''); this.success.set('');
    this.profilSvc.mettreAJour({
      nom: this.editForm.nom || undefined,
      prenom: this.editForm.prenom || undefined,
      telephone: this.editForm.telephone || undefined,
    }).subscribe({
      next: m => {
        this.membre.set(m);
        this.success.set('Profil mis à jour.');
        this.editSection.set(null);
        this.submitting.set(false);
      },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur lors de la mise à jour');
        this.submitting.set(false);
      },
    });
  }

  // ── Changement mot de passe ────────────────────────────────────────────

  ouvrirMdp(): void {
    this.mdpForm = { ancien: '', nouveau: '', confirmation: '' };
    this.editSection.set('mdp');
    this.error.set(''); this.success.set(''); this.mdpError.set('');
  }

  changerMotDePasse(): void {
    this.mdpError.set('');
    if (this.mdpForm.nouveau !== this.mdpForm.confirmation) {
      this.mdpError.set('Les mots de passe ne correspondent pas.'); return;
    }
    if (this.mdpForm.nouveau.length < 8) {
      this.mdpError.set('8 caractères minimum.'); return;
    }
    this.submitting.set(true); this.error.set('');
    this.profilSvc.changerMotDePasse({
      ancienMotDePasse: this.mdpForm.ancien,
      nouveauMotDePasse: this.mdpForm.nouveau,
    }).subscribe({
      next: () => {
        this.success.set('Mot de passe changé avec succès.');
        this.editSection.set(null);
        this.mdpForm = { ancien: '', nouveau: '', confirmation: '' };
        this.submitting.set(false);
      },
      error: e => {
        this.mdpError.set(e.error?.detail ?? e.error?.message ?? 'Erreur');
        this.submitting.set(false);
      },
    });
  }

  // ── 2FA ────────────────────────────────────────────────────────────────

  activer2fa(): void {
    this.submitting.set(true); this.error.set('');
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
        this.success.set('2FA activée !'); this.submitting.set(false);
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
