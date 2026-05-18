import { Component, signal, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

function passwordMatch(control: AbstractControl) {
  const pwd = control.get('password')?.value;
  const confirm = control.get('confirm')?.value;
  return pwd && confirm && pwd !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
})
export class RegisterComponent {
  private fb     = inject(FormBuilder);
  private auth   = inject(AuthService);
  private router = inject(Router);

  form = this.fb.nonNullable.group({
    email:     ['', [Validators.required, Validators.email]],
    telephone: [''],
    password:  ['', [Validators.required, Validators.minLength(8)]],
    confirm:   ['', Validators.required],
  }, { validators: passwordMatch });

  loading  = signal(false);
  error    = signal('');
  showPass = signal(false);

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.error.set('');
    this.loading.set(true);

    const { email, telephone, password } = this.form.getRawValue();

    this.auth.register({ email, password, telephone: telephone || undefined }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err.message ?? 'Erreur lors de la création du compte');
      },
    });
  }
}
