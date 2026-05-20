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
      <p class="text-sm text-gray-500">Gérez votre tontine depuis ce tableau de bord.</p>

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
      desc: 'Taux, périodes, amendes',
      route: '/admin/configuration',
      icon: '⚙️',
      color: 'bg-blue-50',
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
