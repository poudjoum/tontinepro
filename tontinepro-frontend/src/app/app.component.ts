import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/services/auth.service';
import { TontineService } from './core/services/tontine.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private auth    = inject(AuthService);
  private tontineSvc = inject(TontineService);
  private router  = inject(Router);

  // Routes publiques où la vérification ne s'applique pas
  private publicPaths = ['/tontines', '/auth', '/setup', '/rejoindre'];

  private checked = false;

  ngOnInit(): void {
    // Vérification unique au démarrage de l'app pour les MEMBRE sans profil
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: any) => {
      if (this.checked) return;
      if (!this.auth.isLoggedIn() || this.auth.roleGestionnaire() || this.auth.isSuperAdmin()) return;
      if (this.publicPaths.some(p => e.url.startsWith(p))) return;

      // MEMBRE connecté → vérifier profil une seule fois
      this.checked = true;
      // Sans tontine précisée, « mon profil » est ambigu pour un compte présent
      // dans plusieurs tontines : on vérifie seulement qu'il en a au moins une.
      this.tontineSvc.getAll().subscribe({
        next:  list => { if (list.length === 0) this.router.navigate(['/tontines']); },
        error: () => {},
      });
    });
  }
}
