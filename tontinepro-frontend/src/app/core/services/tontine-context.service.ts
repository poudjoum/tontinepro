import { Injectable, computed, inject, signal } from '@angular/core';
import { TontineService } from './tontine.service';
import { AuthService } from './auth.service';
import { TontineResponse } from '../models/tontine.model';

const STORAGE_PREFIX = 'tontineCouranteId';

/** Clé localStorage isolée par compte : deux comptes sur le même navigateur
 *  ne doivent jamais partager la même tontine courante. */
function storageKey(email: string | null): string {
  return email ? `${STORAGE_PREFIX}:${email}` : STORAGE_PREFIX;
}

/**
 * Contexte de la « tontine courante » partagé par tous les menus.
 * Un compte peut appartenir à plusieurs tontines ; cette tontine sélectionnée
 * sert à cloisonner toutes les vues (cotisations, membres, sessions, etc.).
 * La sélection est mémorisée par compte et rechargée dès que le compte connecté change.
 */
@Injectable({ providedIn: 'root' })
export class TontineContextService {
  private tontineSvc = inject(TontineService);
  private auth = inject(AuthService);

  tontines           = signal<TontineResponse[]>([]);
  tontineCouranteId  = signal<string | null>(null);
  tontineCourante    = computed(() =>
    this.tontines().find(t => t.id === this.tontineCouranteId()) ?? null);

  /** Vrai tant que le chargement de la liste des tontines est en cours. */
  chargement = signal(false);

  /** Email du compte pour lequel les tontines ont été chargées (détecte les changements de compte). */
  private loadedForEmail: string | null = null;

  /** Charge les tontines du compte courant et restaure la sélection.
   *  Recharge automatiquement si le compte connecté a changé. */
  init(): void {
    const email = this.auth.currentUser()?.email ?? null;
    if (this.loadedForEmail === email && email !== null) return;

    this.loadedForEmail = email;
    this.chargement.set(true);
    this.tontineSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        const stored = localStorage.getItem(storageKey(email));
        const valide = stored && list.some(t => t.id === stored) ? stored : (list[0]?.id ?? null);
        this.tontineCouranteId.set(valide);
        if (valide) localStorage.setItem(storageKey(email), valide);
        this.chargement.set(false);
      },
      error: () => { this.loadedForEmail = null; this.chargement.set(false); },
    });
  }

  selectionner(id: string): void {
    this.tontineCouranteId.set(id);
    localStorage.setItem(storageKey(this.auth.currentUser()?.email ?? null), id);
  }

  /** À appeler à la déconnexion. */
  reset(): void {
    this.loadedForEmail = null;
    this.tontines.set([]);
    this.tontineCouranteId.set(null);
  }
}
