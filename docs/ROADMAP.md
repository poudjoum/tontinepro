# TontinePro — Roadmap v2 (Plan directeur)

> **Source** : `docs/TontinePro-Roadmap-v2.docx` (Juin 2026, JumpyTech — Confidentiel).
> Ce fichier est la version **vivante et suivable** de la roadmap : il sert de plan
> directeur pour la suite du développement. Mettre à jour la colonne **Statut** à
> chaque feature livrée.
>
> Légende statut : ✅ Fait · 🔨 Partiel · ⬜ À faire · 🔬 Exploration

## 1. Positionnement

TontinePro v1.0 (mai 2026) a déjà des avantages distinctifs face à Djangui, Tontiin,
TontiGo et SmartMifin :

- **Rattrapage historique** (import Excel) — *aucun concurrent ne le propose*.
- **Épargne individuelle** par membre avec capital prêtable.
- **Fond de solidarité** intégré.
- **Transparence membre** (accès autonome aux données).
- **Sécurité** : 2FA, JWT, HTTPS, rôles & permissions.

Deux gaps majeurs à combler en priorité :

1. **Pas d'intégration Mobile Money** (Djangui et Tontiin paient dans l'app).
2. **Pas de présence app store** (Djangui : 50 000 users sur Play Store).

## 2. Matrice de priorisation & suivi

### A — Croissance & Acquisition

| # | Feature | Prio | Impact | Effort | Statut | Notes |
|---|---------|------|--------|--------|--------|-------|
| A1 | Bouton « Recommander TontinePro » dans le dashboard | P1 | Très élevé | Faible | ⬜ | Lien WhatsApp pré-rempli + suivi des recommandations |
| A2 | Parcours « Parrainer une tontine » | P1 | Très élevé | Moyen | ⬜ | Assistant création < 5 min, transfert rôle secrétaire 1 clic, badge Parrain |
| A3 | Compte unique multi-tontines | P2 | Élevé | Élevé | 🔨 | Isolation par appartenance **faite** (commit ccd4c26). Reste : 1 identifiant → N tontines, sélecteur tontine active, vue agrégée |
| A4 | Landing page secrétaire (SEO) | P2 | Élevé | Faible | 🔨 | Landing + section Documentation **faite**. Reste : page dédiée acquisition secrétaire, SEO « njangi digital », « gestion tontine Cameroun » |

### B — Combler les gaps concurrentiels

| # | Feature | Prio | Impact | Effort | Statut | Notes |
|---|---------|------|--------|--------|--------|-------|
| B1 | Intégration Mobile Money (MTN / Orange) | P1 | Très élevé | Élevé | ⬜ | Gap le plus critique. Nécessite agrégateur (CinetPay / Monetbil). Démarrer par étude de faisabilité |
| B2 | Application installable (PWA / Play Store) | P1 | Élevé | Moyen | ⬜ | Manifest + icône + splash + offline partiel + push. Exploration TWA Android |
| B3 | Messagerie interne de groupe | P2 | Moyen | Élevé | ⬜ | Canal par tontine + annonces épinglées. À arbitrer vs WhatsApp externe |

### C — Enrichissement de l'expérience

| # | Feature | Prio | Impact | Effort | Statut | Notes |
|---|---------|------|--------|--------|--------|-------|
| C1 | Procès-verbal de séance digital | P1 | Élevé | Faible | ⬜ | Génération auto après validation de l'appel de présence : présences, cotisations, bénéficiaire, décisions → PDF + WhatsApp |
| C2 | Rapport de fin de session complet | P1 | Élevé | Faible | ✅ | Bilan financier complet (cotisations, repas, fonds, redistribué, épargne, prêts, fonds de solidarité) + fiche individuelle par membre. Page `/rapport-fin-session/:id`, export PDF, email auto à la clôture. Endpoints `GET /sessions/{id}/rapport-fin-session[/pdf]` |
| C3 | Tableau de bord secrétaire amélioré | P2 | Moyen | Moyen | ⬜ | Taux cotisations du mois, total en caisse temps réel, alertes (retards, prêts à échoir, fond bas), comparaison sessions |
| C4 | Attestation membre pour institution bancaire | P2 | Moyen | Faible | ⬜ | Document officiel signé (président/secrétaire) : cotisé, reçu, historique. Différenciant fort |

### D — Exploration stratégique (bets moyen terme)

| # | Feature | Prio | Impact | Effort | Statut | Notes |
|---|---------|------|--------|--------|--------|-------|
| D1 | Tontine diaspora (multi-devises) | P3 | Très élevé | Très élevé | 🔬 | EUR/FCFA, taux fixe à la création, Wave/Stripe. Étude réglementaire requise |
| D2 | Réseau de tontines (annuaire) | P3 | Élevé | Élevé | 🔬 | Tontines « ouvertes » filtrables (ville, montant, fréquence). Modèle freemium |
| D3 | Module de vote et gouvernance | P3 | Moyen | Élevé | 🔬 | Vote anonyme/nominatif, ordre du jour, décisions archivées |

## 3. Séquencement recommandé

**Semaines 1-2 — Zéro-code**
- Tester manuellement le parcours de recommandation auprès des membres actifs.
- Identifier qui a déjà tenté de recommander TontinePro et ce qui a bloqué.
- Rédiger le message type de recommandation WhatsApp (à valider avant de coder A1).

**Semaines 3-6 — Quick wins (faible effort, fort impact)**
- A1 — Bouton Recommander (lien WhatsApp pré-rempli)
- C1 — PV de séance auto après validation de l'appel de présence
- C2 — Rapport de fin de session en PDF (clôture du dernier tour)
- B2 (v1) — Première PWA : manifest + icône installable Android

**Semaines 7-12 — Features structurantes (moyen effort)**
- A2 — Parcours parrainage (badge + historique)
- C3 — Dashboard secrétaire avec indicateurs temps réel
- B1 (étude) — Faisabilité Mobile Money (contacter CinetPay / Monetbil)

**Trimestre 2 — Grands chantiers**
- B1 — Intégration Mobile Money MTN / Orange (si faisabilité confirmée)
- A3 — Compte unique multi-tontines
- A4 — Landing page secrétaire optimisée SEO

## 4. Prochain jalon

✅ **C2 — Rapport de fin de session complet** est livré (juin 2026).

➡️ Prochain candidat : **C1 — PV de séance digital** (faible effort, fort impact),
puis **A1 — Bouton Recommander** (quick win croissance).
