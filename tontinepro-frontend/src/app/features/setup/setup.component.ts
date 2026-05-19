import { Component, signal, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { SetupService } from '../../core/services/setup.service';

function passwordMatch(control: AbstractControl) {
  const pwd = control.get('password')?.value;
  const confirm = control.get('confirm')?.value;
  return pwd && confirm && pwd !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-setup',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './setup.component.html',
})
export class SetupComponent {
  private fb     = inject(FormBuilder);
  private setup  = inject(SetupService);
  private auth   = inject(AuthService);
  private router = inject(Router);

  step     = signal<1 | 2>(1);
  loading  = signal(false);
  error    = signal('');
  showPass = signal(false);

  // Étape 1 — Tontine
  tontineForm = this.fb.nonNullable.group({
    tontineNom:        ['', Validators.required],
    tontineDescription:[''],
    montantCotisationMin: [0, [Validators.required, Validators.min(1)]],
    jourReference:        [1, [Validators.min(1), Validators.max(28)]],
  });

  // Étape 2 — Fondateur
  fondateurForm = this.fb.nonNullable.group({
    nom:      ['', Validators.required],
    prenom:   ['', Validators.required],
    email:    ['', [Validators.required, Validators.email]],
    telephone:[''],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirm:  ['', Validators.required],
  }, { validators: passwordMatch });

  nextStep(): void {
    if (this.tontineForm.invalid) {
      this.tontineForm.markAllAsTouched();
      return;
    }
    this.step.set(2);
  }

  prevStep(): void { this.step.set(1); }

  submit(): void {
    if (this.fondateurForm.invalid || this.loading()) {
      this.fondateurForm.markAllAsTouched();
      return;
    }
    this.error.set('');
    this.loading.set(true);

    const t = this.tontineForm.getRawValue();
    const f = this.fondateurForm.getRawValue();

    this.setup.setup({
      nom: f.nom, prenom: f.prenom, email: f.email,
      password: f.password, telephone: f.telephone || undefined,
      tontineNom: t.tontineNom, tontineDescription: t.tontineDescription || undefined,
      montantCotisationMin: t.montantCotisationMin,
      jourReference: t.jourReference || undefined,
    }).subscribe({
      next: authResp => {
        this.loading.set(false);
        this.auth.saveAuth(authResp);
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err.message ?? 'Erreur lors de la configuration');
      },
    });
  }
}
