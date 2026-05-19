import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InvitationService } from '../../../core/services/invitation.service';
import { TontineService } from '../../../core/services/tontine.service';
import { InvitationResponse } from '../../../core/models/invitation.model';
import { TontineResponse } from '../../../core/models/tontine.model';

@Component({
  selector: 'app-invitations',
  imports: [FormsModule],
  templateUrl: './invitations.component.html',
})
export class InvitationsComponent implements OnInit {
  private svc     = inject(InvitationService);
  private tontSvc = inject(TontineService);

  tontines     = signal<TontineResponse[]>([]);
  invitations  = signal<InvitationResponse[]>([]);
  loading      = signal(true);
  generating   = signal(false);
  error        = signal('');
  success      = signal('');
  lienCopie    = signal('');

  tontineId  = '';
  dureeHeures = 72;
  now = new Date();

  actives = computed(() => this.invitations().filter(i => !i.utilise && new Date(i.expiresAt) > new Date()));

  ngOnInit(): void {
    this.tontSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        if (list[0]) this.tontineId = list[0].id;
        this.chargerInvitations();
      },
      error: () => this.loading.set(false),
    });
  }

  chargerInvitations(): void {
    this.svc.lister(this.tontineId || undefined).subscribe({
      next: inv => { this.invitations.set(inv); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  generer(): void {
    if (!this.tontineId) return;
    this.generating.set(true);
    this.success.set('');
    this.error.set('');

    this.svc.generer({ tontineId: this.tontineId, dureeHeures: this.dureeHeures }).subscribe({
      next: inv => {
        this.invitations.update(list => [inv, ...list]);
        this.success.set('Lien d\'invitation généré avec succès !');
        this.generating.set(false);
      },
      error: e => {
        this.error.set(e.error?.message ?? 'Erreur lors de la génération');
        this.generating.set(false);
      },
    });
  }

  lienComplet(token: string): string {
    return `${window.location.origin}/rejoindre/${token}`;
  }

  copier(token: string): void {
    navigator.clipboard.writeText(this.lienComplet(token)).then(() => {
      this.lienCopie.set(token);
      setTimeout(() => this.lienCopie.set(''), 2000);
    });
  }

  estExpire(expiresAt: string): boolean {
    return new Date(expiresAt) <= this.now;
  }

  estActif(inv: { utilise: boolean; expiresAt: string }): boolean {
    return !inv.utilise && !this.estExpire(inv.expiresAt);
  }

  expireIn(expiresAt: string): string {
    const diff = new Date(expiresAt).getTime() - Date.now();
    if (diff <= 0) return 'Expiré';
    const h = Math.floor(diff / 3600000);
    if (h < 48) return `${h}h restantes`;
    return `${Math.floor(h / 24)}j restants`;
  }
}
