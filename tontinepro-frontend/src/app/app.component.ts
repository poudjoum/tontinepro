import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './core/services/auth.service';
import { MembreService } from './core/services/membre.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private auth    = inject(AuthService);
  private mbrSvc  = inject(MembreService);
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
      if (!this.auth.isLoggedIn() || this.auth.isGestionnaire() || this.auth.isSuperAdmin()) return;
      if (this.publicPaths.some(p => e.url.startsWith(p))) return;

      // MEMBRE connecté → vérifier profil une seule fois
      this.checked = true;
      this.mbrSvc.getMonProfil().subscribe({
        next:  () => {},  // profil OK → pas de redirection
        error: () => this.router.navigate(['/tontines']),
      });
    });
  }
}
