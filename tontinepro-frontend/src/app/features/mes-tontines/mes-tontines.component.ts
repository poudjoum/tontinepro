import { Component, OnInit, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TontineContextService } from '../../core/services/tontine-context.service';

/**
 * Écran d'accueil après connexion : liste toutes les tontines auxquelles le
 * compte appartient. L'utilisateur en choisit une avant de la consulter.
 * La sélection alimente le TontineContextService (tontine courante) partagé
 * par toutes les vues, puis on redirige vers le tableau de bord.
 */
@Component({
  selector: 'app-mes-tontines',
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-primary-50 to-primary-100 p-4">
      <div class="max-w-2xl mx-auto space-y-4">

        <!-- Header -->
        <div class="flex items-center justify-between pt-2">
          <div class="flex items-center gap-2">
            <div class="w-9 h-9 bg-primary-600 rounded-xl flex items-center justify-center">
              <span class="text-white font-bold">T</span>
            </div>
            <span class="font-bold text-gray-900">TontinePro</span>
          </div>
          <button type="button" (click)="deconnexion()"
                  class="text-sm font-semibold text-gray-500 border border-gray-300
                         px-3 py-1.5 rounded-lg">
            Déconnexion
          </button>
        </div>

        <div class="text-center py-2">
          <h1 class="text-xl font-bold text-gray-900">Bonjour 👋</h1>
          <p class="text-sm text-gray-500 mt-1">
            @if (tontines().length > 0) {
              Choisissez la tontine que vous souhaitez consulter.
            } @else {
              {{ auth.currentUser()?.email }}
            }
          </p>
        </div>

        @if (loading()) {
          <div class="text-center py-8 text-gray-400">Chargement…</div>
        } @else if (tontines().length > 0) {
          <div class="space-y-3">
            @for (t of tontines(); track t.id) {
              <button type="button" (click)="choisir(t.id)"
                 class="w-full text-left block bg-white rounded-2xl p-4 shadow-sm border border-gray-100
                        active:scale-98 transition-transform">
                <div class="flex items-center justify-between gap-3">
                  <div class="flex items-center gap-3 min-w-0">
                    <div class="w-11 h-11 rounded-xl bg-primary-100 flex items-center justify-center flex-shrink-0">
                      <span class="text-primary-700 font-bold uppercase">{{ t.nom.charAt(0) }}</span>
                    </div>
                    <div class="min-w-0">
                      <h2 class="text-base font-bold text-gray-900 truncate">{{ t.nom }}</h2>
                      @if (t.description) {
                        <p class="text-xs text-gray-500 line-clamp-1">{{ t.description }}</p>
                      }
                      <p class="text-xs text-gray-400 mt-0.5">
                        Cotisation min. {{ fcfa(t.montantCotisationMin) }}
                      </p>
                    </div>
                  </div>
                  <svg class="w-5 h-5 text-gray-300 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                  </svg>
                </div>
              </button>
            }
          </div>
        } @else {
          <div class="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 text-center space-y-3">
            <p class="text-gray-700 font-semibold">Vous n'appartenez à aucune tontine</p>
            <p class="text-sm text-gray-500">
              Rejoignez une tontine existante ou créez la vôtre pour commencer.
            </p>
            <div class="space-y-2 pt-1">
              <a routerLink="/tontines"
                 class="block text-center py-3 rounded-xl bg-primary-600 text-white font-semibold text-sm">
                Découvrir les tontines
              </a>
              <a routerLink="/creer-tontine"
                 class="block text-center py-3 rounded-xl border-2 border-primary-600 text-primary-700 font-semibold text-sm">
                + Créer ma propre tontine
              </a>
            </div>
          </div>
        }
      </div>
    </div>
  `,
})
export class MesTontinesComponent implements OnInit {
  auth = inject(AuthService);
  private ctx    = inject(TontineContextService);
  private router = inject(Router);

  tontines = computed(() => this.ctx.tontines());
  loading  = computed(() => this.ctx.chargement());

  ngOnInit(): void {
    this.ctx.init();
  }

  choisir(id: string): void {
    this.ctx.selectionner(id);
    this.router.navigate(['/dashboard']);
  }

  deconnexion(): void {
    this.auth.logout();
  }

  fcfa(n: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }
}
