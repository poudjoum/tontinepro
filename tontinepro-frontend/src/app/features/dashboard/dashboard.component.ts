import { Component, OnInit, signal, inject } from '@angular/core';
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

  membre  = signal<MembreDashboard | null>(null);
  admin   = signal<AdminDashboard | null>(null);
  loading = signal(true);
  error   = signal('');

  ngOnInit(): void {
    if (this.auth.isGestionnaire()) {
      this.svc.getAdminDashboard().subscribe({
        next:  d => { this.admin.set(d);  this.loading.set(false); },
        error: e => { this.error.set(e.message); this.loading.set(false); },
      });
    }
    this.svc.getMembreDashboard().subscribe({
      next:  d => { this.membre.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
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
