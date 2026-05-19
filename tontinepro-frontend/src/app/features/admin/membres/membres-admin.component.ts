import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MembreService } from '../../../core/services/membre.service';
import { TontineService } from '../../../core/services/tontine.service';
import { MembreResponse } from '../../../core/models/membre.model';
import { TontineResponse } from '../../../core/models/tontine.model';
import { environment } from '../../../../environments/environment';

type Fonction = 'PRESIDENT' | 'SECRETAIRE' | 'TRESORIER' | 'CENSEUR' | 'MEMBRE_ORDINAIRE';

@Component({
  selector: 'app-membres-admin',
  imports: [FormsModule],
  templateUrl: './membres-admin.component.html',
})
export class MembresAdminComponent implements OnInit {
  private mbrSvc  = inject(MembreService);
  private tontSvc = inject(TontineService);
  private http    = inject(HttpClient);

  tontines   = signal<TontineResponse[]>([]);
  membres    = signal<MembreResponse[]>([]);
  loading    = signal(true);
  saving     = signal<string | null>(null);
  inscribing = signal(false);
  error      = signal('');
  success    = signal('');
  showForm   = signal(false);

  tontineId = '';
  recherche = '';

  form = {
    nom: '',
    prenom: '',
    email: '',
    password: '',
    telephone: '',
    fonction: 'MEMBRE_ORDINAIRE' as Fonction,
  };

  fonctions: { value: Fonction; label: string }[] = [
    { value: 'PRESIDENT',        label: 'Président' },
    { value: 'SECRETAIRE',       label: 'Secrétaire' },
    { value: 'TRESORIER',        label: 'Trésorier' },
    { value: 'CENSEUR',          label: 'Censeur' },
    { value: 'MEMBRE_ORDINAIRE', label: 'Membre ordinaire' },
  ];

  filtres = computed(() => {
    const q = this.recherche.toLowerCase();
    return this.membres().filter(m =>
      !q || m.nom.toLowerCase().includes(q) || m.prenom.toLowerCase().includes(q) || m.matricule.toLowerCase().includes(q)
    );
  });

  ngOnInit(): void {
    this.tontSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        if (list[0]) { this.tontineId = list[0].id; this.charger(); }
        else this.loading.set(false);
      },
    });
  }

  charger(): void {
    if (!this.tontineId) return;
    this.loading.set(true);
    this.mbrSvc.getAll(this.tontineId).subscribe({
      next: m => { this.membres.set(m); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  inscrire(): void {
    if (!this.tontineId) return;
    this.inscribing.set(true);
    this.error.set('');
    this.success.set('');

    this.http.post<MembreResponse>(
      `${environment.apiUrl}/membres/inscription-directe`,
      { ...this.form, tontineId: this.tontineId }
    ).subscribe({
      next: m => {
        this.membres.update(list => [...list, m]);
        this.success.set(`${m.prenom} ${m.nom} inscrit(e) — matricule : ${m.matricule}`);
        this.form = { nom: '', prenom: '', email: '', password: '', telephone: '', fonction: 'MEMBRE_ORDINAIRE' };
        this.showForm.set(false);
        this.inscribing.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors de l\'inscription');
        this.inscribing.set(false);
      },
    });
  }

  changerFonction(id: string, event: Event): void {
    const val = (event.target as HTMLSelectElement).value as Fonction;
    this.saving.set(id);
    this.mbrSvc.updateFonction(id, val).subscribe({
      next: updated => { this.membres.update(list => list.map(m => m.id === id ? updated : m)); this.saving.set(null); },
      error: e => { this.error.set(e.error?.message ?? 'Erreur'); this.saving.set(null); },
    });
  }

  changerStatut(id: string, statut: string): void {
    this.saving.set(id);
    this.mbrSvc.updateStatut(id, statut).subscribe({
      next: updated => { this.membres.update(list => list.map(m => m.id === id ? updated : m)); this.saving.set(null); },
      error: e => { this.error.set(e.error?.message ?? 'Erreur'); this.saving.set(null); },
    });
  }

  labelFonction(f: string): string {
    return this.fonctions.find(x => x.value === f)?.label ?? f;
  }
}
