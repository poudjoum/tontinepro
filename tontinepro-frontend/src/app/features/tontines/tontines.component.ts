import { Component, OnInit, signal, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TontineService } from '../../core/services/tontine.service';
import { TontineResponse } from '../../core/models/tontine.model';

@Component({
  selector: 'app-tontines',
  imports: [RouterLink],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-primary-50 to-primary-100 p-4">
      <div class="max-w-2xl mx-auto space-y-4">

        <div class="text-center pt-4 pb-2">
          <div class="inline-flex items-center justify-center w-14 h-14 bg-primary-600 rounded-2xl mb-3">
            <span class="text-white text-xl font-bold">T</span>
          </div>
          <h1 class="text-2xl font-bold text-gray-900">TontinePro</h1>
          <p class="text-sm text-gray-500 mt-1">Tontines disponibles</p>
        </div>

        @if (loading()) {
          <div class="text-center py-8 text-gray-400">Chargement…</div>
        } @else {
          <div class="space-y-3">
            @for (t of tontines(); track t.id) {
              <a [routerLink]="['/tontines', t.id]"
                 class="block bg-white rounded-2xl p-4 shadow-sm border border-gray-100 active:scale-98 transition-transform">
                <div class="flex items-start justify-between gap-3">
                  <div class="flex-1">
                    <div class="flex items-center gap-2 mb-1">
                      <h2 class="text-base font-bold text-gray-900">{{ t.nom }}</h2>
                      @if (t.typeAcces === 'RESTREINTE') {
                        <span class="text-[10px] font-medium px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">Groupe restreint</span>
                      } @else {
                        <span class="text-[10px] font-medium px-2 py-0.5 rounded-full bg-green-100 text-green-700">Ouverte</span>
                      }
                    </div>
                    @if (t.description) {
                      <p class="text-xs text-gray-500 line-clamp-2">{{ t.description }}</p>
                    }
                    @if (t.descriptionAcces) {
                      <p class="text-xs text-amber-700 mt-1 italic">{{ t.descriptionAcces }}</p>
                    }
                    <div class="flex items-center gap-3 mt-2 text-xs text-gray-500">
                      <span>Min {{ fcfa(t.montantCotisationMin) }}</span>
                      <span>·</span>
                      <span>{{ labelPeriode(t.typeReglePeriodicite) }}</span>
                    </div>
                  </div>
                  <svg class="w-5 h-5 text-gray-300 flex-shrink-0 mt-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                  </svg>
                </div>
              </a>
            } @empty {
              <div class="text-center py-10 text-gray-400">
                <p class="text-lg">Aucune tontine disponible</p>
                <p class="text-sm mt-1">Revenez ultérieurement.</p>
              </div>
            }
          </div>

          <div class="text-center pt-2">
            <a routerLink="/auth/login" class="text-sm text-primary-600 font-medium">
              Déjà membre ? Se connecter →
            </a>
          </div>
        }
      </div>
    </div>
  `,
})
export class TontinesComponent implements OnInit {
  private svc = inject(TontineService);
  tontines = signal<TontineResponse[]>([]);
  loading  = signal(true);

  ngOnInit(): void {
    this.svc.getPubliques().subscribe({
      next: list => { this.tontines.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  fcfa(n: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(n) + ' FCFA';
  }

  labelPeriode(r: string): string {
    const map: Record<string, string> = {
      HEBDOMADAIRE: 'Hebdo', MENSUEL_JOUR_FIXE: 'Mensuel',
      MENSUEL_DERNIER_SAMEDI: 'Dernier samedi', MENSUEL_DERNIER_DIMANCHE: 'Dernier dimanche',
      MENSUEL_PREMIER_DIMANCHE_APRES: '1er dim. après', MENSUEL_DIMANCHE_SUR_OU_APRES: 'Dim. ou après',
      TRIMESTRIEL_JOUR_FIXE: 'Trimestriel', SEMESTRIEL_JOUR_FIXE: 'Semestriel',
      ANNUEL_JOUR_FIXE: 'Annuel', DATE_MANUELLE: 'Date variable',
    };
    return map[r] ?? r;
  }
}
