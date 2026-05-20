import { Component, signal, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PostAuthNavService } from '../../../core/services/post-auth-nav.service';

@Component({
  selector: 'app-two-fa',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './two-fa.component.html',
})
export class TwoFaComponent implements OnInit {
  private fb      = inject(FormBuilder);
  private auth    = inject(AuthService);
  private router  = inject(Router);
  private postNav = inject(PostAuthNavService);

  form = this.fb.nonNullable.group({
    code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  loading = signal(false);
  error   = signal('');
  email   = signal('');
  ticket  = signal('');

  ngOnInit(): void {
    const state = history.state as { ticket?: string; email?: string };
    if (!state?.ticket) {
      this.router.navigate(['/auth/login']);
      return;
    }
    this.ticket.set(state.ticket);
    this.email.set(state.email ?? '');
  }

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.error.set('');
    this.loading.set(true);

    this.auth.validate2fa(this.ticket(), this.form.getRawValue().code).subscribe({
      next: () => {
        this.loading.set(false);
        this.postNav.navigateAfterLogin();
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err.message ?? 'Code invalide ou expiré');
        this.form.reset();
      },
    });
  }
}
