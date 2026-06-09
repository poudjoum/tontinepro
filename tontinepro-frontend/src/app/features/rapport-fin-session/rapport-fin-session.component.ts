import { Component, OnInit, signal, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { SessionService } from '../../core/services/session.service';
import { RapportFinSessionResponse } from '../../core/models/session.model';

@Component({
  selector: 'app-rapport-fin-session',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './rapport-fin-session.component.html',
})
export class RapportFinSessionComponent implements OnInit {
  private route      = inject(ActivatedRoute);
  private sessionSvc = inject(SessionService);

  rapport  = signal<RapportFinSessionResponse | null>(null);
  loading  = signal(true);
  error    = signal('');
  today    = new Date().toISOString();

  ngOnInit(): void {
    const sessionId = this.route.snapshot.paramMap.get('sessionId')!;
    this.sessionSvc.getRapportFinSession(sessionId).subscribe({
      next:  r => { this.rapport.set(r); this.loading.set(false); },
      error: e => { this.error.set(e.error?.detail ?? e.error?.message ?? 'Erreur chargement'); this.loading.set(false); },
    });
  }

  statutLabel(s: string): string {
    return s === 'TERMINEE' ? 'Session clôturée' : s === 'EN_COURS' ? 'Session en cours' : s;
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  formatDate(d: string | null | undefined): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  imprimer(): void { window.print(); }

  telechargerPdf(): void {
    const r = this.rapport();
    if (!r) return;
    this.sessionSvc.telechargerRapportFinSessionPdf(r.sessionId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `rapport-fin-session-${r.sessionNumero}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error.set('Erreur téléchargement PDF'),
    });
  }
}
