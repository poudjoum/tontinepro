import { Injectable, computed, inject, signal } from '@angular/core';
import { TontineService } from './tontine.service';
import { TontineResponse } from '../models/tontine.model';

const STORAGE_KEY = 'tontineCouranteId';

/**
 * Contexte de la « tontine courante » partagé par tous les menus.
 * Un compte peut appartenir à plusieurs tontines ; cette tontine sélectionnée
 * sert à cloisonner toutes les vues (cotisations, membres, sessions, etc.).
 */
@Injectable({ providedIn: 'root' })
export class TontineContextService {
  private tontineSvc = inject(TontineService);

  tontines           = signal<TontineResponse[]>([]);
  tontineCouranteId  = signal<string | null>(null);
  tontineCourante    = computed(() =>
    this.tontines().find(t => t.id === this.tontineCouranteId()) ?? null);

  private loaded = false;

  /** Charge une seule fois les tontines du compte et restaure la sélection. */
  init(): void {
    if (this.loaded) return;
    this.loaded = true;
    this.tontineSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        const stored = localStorage.getItem(STORAGE_KEY);
        const valide = stored && list.some(t => t.id === stored) ? stored : (list[0]?.id ?? null);
        this.tontineCouranteId.set(valide);
        if (valide) localStorage.setItem(STORAGE_KEY, valide);
      },
      error: () => { this.loaded = false; },
    });
  }

  selectionner(id: string): void {
    this.tontineCouranteId.set(id);
    localStorage.setItem(STORAGE_KEY, id);
  }

  /** À appeler à la déconnexion. */
  reset(): void {
    this.loaded = false;
    this.tontines.set([]);
    this.tontineCouranteId.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }
}
