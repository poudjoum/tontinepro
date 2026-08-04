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

  /**
   * Amorcé depuis localStorage plutôt qu'à null : les gardes de route lisent ce
   * signal de façon synchrone, bien avant que `init()` n'ait reçu sa réponse.
   * Sans valeur initiale, tout accès à froid — URL collée, favori, F5 — à un
   * écran gardé rebondissait vers la sélection de tontine. La valeur restaurée
   * est provisoire : `init()` la revalide contre la liste réelle des tontines
   * du compte et la corrige si elle n'y figure pas.
   */
  tontineCouranteId  = signal<string | null>(
    localStorage.getItem(storageKey(this.auth.currentUser()?.email ?? null)));
  tontineCourante    = computed(() =>
    this.tontines().find(t => t.id === this.tontineCouranteId()) ?? null);

  /** Vrai tant que le chargement de la liste des tontines est en cours. */
  chargement = signal(false);

  /**
   * Vrai si le dernier chargement a échoué. Sans ce drapeau, une liste vide par
   * erreur réseau est indiscernable d'une liste vide légitime, et l'interface
   * affirme à tort que le compte n'appartient à aucune tontine.
   */
  erreurChargement = signal(false);

  /** Email du compte pour lequel les tontines ont été chargées (détecte les changements de compte). */
  private loadedForEmail: string | null = null;

  /** Charge les tontines du compte courant et restaure la sélection.
   *  Recharge automatiquement si le compte connecté a changé. */
  init(): void {
    const email = this.auth.currentUser()?.email ?? null;
    if (this.loadedForEmail === email && email !== null) return;

    this.loadedForEmail = email;
    this.chargement.set(true);
    this.erreurChargement.set(false);
    this.tontineSvc.getAll().subscribe({
      next: list => {
        this.tontines.set(list);
        const stored = localStorage.getItem(storageKey(email));
        const valide = stored && list.some(t => t.id === stored) ? stored : (list[0]?.id ?? null);
        this.tontineCouranteId.set(valide);
        if (valide) localStorage.setItem(storageKey(email), valide);
        this.chargement.set(false);
      },
      error: () => {
        this.loadedForEmail = null;
        this.erreurChargement.set(true);
        this.chargement.set(false);
      },
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
