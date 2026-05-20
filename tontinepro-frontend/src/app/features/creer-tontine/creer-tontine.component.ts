import { Component, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { TontineService } from '../../core/services/tontine.service';
import { AuthService } from '../../core/services/auth.service';

type Etape = 'tontine' | 'compte' | 'confirmation';

@Component({
  selector: 'app-creer-tontine',
  imports: [FormsModule, RouterLink, DecimalPipe],
  templateUrl: './creer-tontine.component.html',
})
export class CreerTontineComponent {
  private tontineSvc = inject(TontineService);
  private authSvc    = inject(AuthService);
  private router     = inject(Router);

  etape      = signal<Etape>('tontine');
  submitting = signal(false);
  error      = signal('');

  // Déjà connecté ?
  dejaConnecte = this.authSvc.isLoggedIn;

  // Formulaire tontine
  tontine = {
    nom: '',
    description: '',
    montantCotisationMin: 5000,
    typeAcces: 'OUVERTE' as 'OUVERTE' | 'RESTREINTE',
    descriptionAcces: '',
  };

  // Formulaire compte (si pas connecté)
  compte = {
    nom: '',
    prenom: '',
    email: '',
    password: '',
    telephone: '',
  };

  // Formulaire membres (si déjà connecté)
  membre = { nom: '', prenom: '' };

  avancer(): void {
    if (this.etape() === 'tontine') {
      if (!this.tontine.nom || !this.tontine.montantCotisationMin) {
        this.error.set('Veuillez remplir le nom et le montant minimum.');
        return;
      }
      this.error.set('');
      this.etape.set(this.dejaConnecte() ? 'confirmation' : 'compte');
    }
  }

  creer(): void {
    this.submitting.set(true);
    this.error.set('');

    if (this.dejaConnecte()) {
      // Utilisateur connecté sans profil
      this.tontineSvc.creerMaTontine({
        nom: this.tontine.nom,
        description: this.tontine.description || undefined,
        montantCotisationMin: this.tontine.montantCotisationMin,
        typeAcces: this.tontine.typeAcces,
        descriptionAcces: this.tontine.descriptionAcces || undefined,
        membreNom: this.membre.nom,
        membrePrenom: this.membre.prenom,
      }).subscribe({
        next: () => {
          this.submitting.set(false);
          this.router.navigate(['/admin/onboarding']);
        },
        error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur création'); this.submitting.set(false); },
      });
    } else {
      // Création depuis zéro (compte + tontine)
      this.tontineSvc.creerAvecCompte({
        email: this.compte.email,
        password: this.compte.password,
        nom: this.compte.nom,
        prenom: this.compte.prenom,
        telephone: this.compte.telephone || undefined,
        tontineNom: this.tontine.nom,
        tontineDescription: this.tontine.description || undefined,
        montantCotisationMin: this.tontine.montantCotisationMin,
        typeAcces: this.tontine.typeAcces,
        descriptionAcces: this.tontine.descriptionAcces || undefined,
      }).subscribe({
        next: auth => {
          this.authSvc.saveAuth(auth);
          this.submitting.set(false);
          this.router.navigate(['/admin/onboarding']);
        },
        error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur création'); this.submitting.set(false); },
      });
    }
  }

  retour(): void {
    this.etape.set('tontine');
    this.error.set('');
  }
}
