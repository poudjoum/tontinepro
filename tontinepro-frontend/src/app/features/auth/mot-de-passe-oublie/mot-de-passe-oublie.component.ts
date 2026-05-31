import { Component, signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-mot-de-passe-oublie',
  imports: [FormsModule, RouterLink],
  templateUrl: './mot-de-passe-oublie.component.html',
})
export class MotDePasseOublieComponent {
  private http = inject(HttpClient);
  private api  = `${environment.apiUrl}/auth/mot-de-passe-oublie`;

  form = { nom: '', prenom: '', email: '', telephone: '' };

  submitting = signal(false);
  success    = signal(false);
  error      = signal('');

  soumettre(): void {
    const { nom, prenom, email } = this.form;
    if (!nom || !prenom || !email) {
      this.error.set('Veuillez remplir les champs obligatoires (nom, prénom, email).');
      return;
    }
    this.submitting.set(true);
    this.error.set('');
    this.http.post<any>(this.api, {
      nom: this.form.nom.trim(),
      prenom: this.form.prenom.trim(),
      email: this.form.email.trim().toLowerCase(),
      telephone: this.form.telephone || undefined,
    }).subscribe({
      next: () => { this.success.set(true); this.submitting.set(false); },
      error: e => {
        this.error.set(e.error?.detail ?? e.error?.message ?? 'Une erreur est survenue. Réessayez.');
        this.submitting.set(false);
      },
    });
  }
}
