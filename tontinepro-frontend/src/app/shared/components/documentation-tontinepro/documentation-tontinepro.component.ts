import { Component } from '@angular/core';

interface DocumentationTontinePro {
  url: string;
  titre: string;
  description: string;
  icone: string;
  fondIcone: string;
  format: 'PDF' | 'PPTX';
}

/**
 * Documentation TontinePro (guide utilisateur, présentations) servie depuis
 * `public/docs`. Affichée sur la page d'accueil publique et dans l'écran Documents.
 */
@Component({
  selector: 'app-documentation-tontinepro',
  template: `
    <div class="flex flex-col gap-3">
      @for (doc of documents; track doc.url) {
        <a [href]="doc.url" target="_blank" rel="noopener"
           class="card flex items-center gap-4 p-4 active:bg-gray-50 transition-colors">
          <div class="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0" [class]="doc.fondIcone">
            <span class="text-2xl">{{ doc.icone }}</span>
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-semibold text-sm text-gray-900">{{ doc.titre }}</p>
            <p class="text-xs text-gray-500 mt-0.5">{{ doc.description }}</p>
            <span class="inline-block mt-1 text-[10px] font-semibold px-2 py-0.5 rounded"
                  [class.bg-red-100]="doc.format === 'PDF'" [class.text-red-700]="doc.format === 'PDF'"
                  [class.bg-orange-100]="doc.format === 'PPTX'" [class.text-orange-700]="doc.format === 'PPTX'">
              {{ doc.format }}
            </span>
          </div>
          <svg class="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
        </a>
      }
    </div>
  `,
})
export class DocumentationTontineProComponent {
  readonly documents: DocumentationTontinePro[] = [
    {
      url: '/docs/TontinePro-Guide-Utilisateur.pdf',
      titre: 'Guide utilisateur',
      description: 'Connexion, cotisations, épargne, prêts, aides, administration, tontine à lot',
      icone: '📕', fondIcone: 'bg-red-50', format: 'PDF',
    },
    {
      url: '/docs/TontinePro-Presentation.pdf',
      titre: 'Présentation TontinePro',
      description: 'Fonctionnalités, tontine à lot, sécurité — à partager avec vos membres',
      icone: '📊', fondIcone: 'bg-purple-50', format: 'PDF',
    },
    {
      url: '/docs/TontinePro-Presentation.pptx',
      titre: 'Présentation PowerPoint',
      description: '9 diapositives — à ouvrir dans PowerPoint ou Google Slides',
      icone: '🎯', fondIcone: 'bg-orange-50', format: 'PPTX',
    },
  ];
}
