import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { MembreDashboard, AdminDashboard } from '../../core/models/dashboard.model';

const MOIS_FR = ['', 'Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin',
                 'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit {
  auth    = inject(AuthService);
  private svc = inject(DashboardService);

  membre   = signal<MembreDashboard | null>(null);
  admin    = signal<AdminDashboard | null>(null);
  error    = signal('');
  sansProfil = signal(false); // user connecté mais sans profil membre (→ Bienvenue)

  private pendingCalls = signal(0);
  loading = computed(() => this.pendingCalls() > 0);

  ngOnInit(): void {
    const callsToMake = this.auth.isGestionnaire() ? 2 : 1;
    this.pendingCalls.set(callsToMake);

    if (this.auth.isGestionnaire()) {
      this.svc.getAdminDashboard().subscribe({
        next: d => { this.admin.set(d); this.pendingCalls.update(n => n - 1); },
        error: () => { this.pendingCalls.update(n => n - 1); },
      });
    }

    this.svc.getMembreDashboard().subscribe({
      next:  d => {
        this.membre.set(d);
        this.pendingCalls.update(n => n - 1);
      },
      error: e => {
        // 400/404 = pas de profil membre ; on affiche le panneau "Bienvenue"
        if (e.status === 400 || e.status === 404) {
          this.sansProfil.set(true);
        }
        this.pendingCalls.update(n => n - 1);
      },
    });
  }

  fcfa(n: number | null | undefined): string {
    if (n == null) return '—';
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  moisNom(m: number | null): string {
    return m ? (MOIS_FR[m] ?? String(m)) : '—';
  }

  tauxRecouvrement(a: AdminDashboard): number {
    if (!a.membresActifs) return 0;
    return Math.round((a.cotisationsPayees / a.membresActifs) * 100);
  }
}
