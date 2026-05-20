import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface AdminCard {
  title: string;
  desc: string;
  route: string;
  icon: string;
  color: string;
}

@Component({
  selector: 'app-admin',
  imports: [RouterLink],
  template: `
    <div class="space-y-4">
      <h1 class="text-xl font-bold text-gray-900">Administration</h1>

      <!-- Démarrage rapide -->
      <a routerLink="/admin/onboarding"
         class="block bg-gradient-to-r from-primary-600 to-primary-700 rounded-xl p-4 text-white shadow">
        <div class="flex items-center gap-3">
          <span class="text-3xl">🚀</span>
          <div>
            <p class="font-bold text-sm">Guide de démarrage</p>
            <p class="text-xs text-primary-200 mt-0.5">
              Documents, configuration, membres, bureau, session
            </p>
          </div>
          <svg class="w-5 h-5 text-primary-200 ml-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </div>
      </a>

      <div class="grid grid-cols-2 gap-3">
        @for (card of cards; track card.route) {
          <a [routerLink]="card.route"
             class="bg-white rounded-xl p-4 shadow-sm border border-gray-100 flex flex-col gap-2 active:scale-95 transition-transform">
            <div [class]="'w-10 h-10 rounded-lg flex items-center justify-center ' + card.color">
              <span class="text-xl">{{ card.icon }}</span>
            </div>
            <div>
              <p class="text-sm font-semibold text-gray-900">{{ card.title }}</p>
              <p class="text-[11px] text-gray-500 leading-snug mt-0.5">{{ card.desc }}</p>
            </div>
          </a>
        }
      </div>
    </div>
  `,
})
export class AdminComponent {
  cards: AdminCard[] = [
    {
      title: 'Configuration',
      desc: 'Taux, périodes, fonds d\'aide',
      route: '/admin/configuration',
      icon: '⚙️',
      color: 'bg-blue-50',
    },
    {
      title: 'Documents',
      desc: 'Statuts, règlement intérieur',
      route: '/admin/documents-tontine',
      icon: '📄',
      color: 'bg-gray-50',
    },
    {
      title: 'Membres',
      desc: 'Gérer les membres et fonctions',
      route: '/admin/membres',
      icon: '👥',
      color: 'bg-green-50',
    },
    {
      title: 'Invitations',
      desc: 'Liens d\'adhésion',
      route: '/admin/invitations',
      icon: '🔗',
      color: 'bg-purple-50',
    },
    {
      title: 'Absences',
      desc: 'Enregistrer les absences',
      route: '/admin/absences',
      icon: '📋',
      color: 'bg-yellow-50',
    },
    {
      title: 'Sanctions',
      desc: 'Amendes et pénalités',
      route: '/admin/sanctions',
      icon: '⚠️',
      color: 'bg-red-50',
    },
    {
      title: 'Sessions',
      desc: 'Rotation et bénéficiaires',
      route: '/admin/sessions',
      icon: '🔄',
      color: 'bg-teal-50',
    },
    {
      title: 'Demandes',
      desc: 'Adhésions à approuver',
      route: '/admin/demandes',
      icon: '📨',
      color: 'bg-orange-50',
    },
    {
      title: 'Rapports',
      desc: 'PDF et Excel',
      route: '/rapports',
      icon: '📊',
      color: 'bg-indigo-50',
    },
  ];
}
