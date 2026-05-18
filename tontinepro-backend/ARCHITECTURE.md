# Architecture Technique — TontinePro

---

## Vue d'Ensemble

Ce document présente l'architecture technique complète de **TontinePro**, une plateforme numérique conçue pour gérer les opérations de tontine incluant la gestion des membres, les cotisations, l'entraide mutuelle, l'épargne et les prêts. Le système est construit avec une approche moderne et mobile-first, utilisant une architecture frontend-backend découplée.

---

## Pile Technologique

### Frontend

- **Framework :** Angular (Composants Standalone)
- **Stratégie Mobile :** Progressive Web App (PWA) + Capacitor.js
- **Stylisation :** SCSS avec breakpoints mobile-first
- **Gestion d'état :** Services Angular (Pattern Singleton)
- **Routage :** Modules lazy-loaded par fonctionnalité

### Backend — Spring Boot (Java 21)

- **Runtime :** JVM
- **Langage :** Java 21
- **Framework :** Spring Boot 4.0.6
- **ORM :** Spring Data JPA + Hibernate 7
- **Sécurité :** Spring Security 7 (stateless JWT)
- **Migrations :** Flyway 11
- **Architecture :** DDD — couches `domain` / `api` / `infrastructure`

### Couche de Données

- **Base de Données Principale :** PostgreSQL 16
- **Cache & Sessions :** Redis 7
- **Stockage de Fichiers :** MinIO / AWS S3
- **Passerelle API :** Nginx (Load balancing + Terminaison SSL)

### Services Tiers

- **Fournisseurs de Paiement :** CinetPay, MTN MoMo, Orange Money
- **SMS :** Africa's Talking
- **Email :** Mailgun / SendGrid
- **Notifications Push :** Firebase Cloud Messaging

---

## Diagramme d'Architecture Système

```
┌─────────────────────────────────────────────────────────┐
│                      CLIENTS                            │
│                                                         │
│   Angular PWA (Web + Mobile First)                      │
│   Capacitor.js (iOS / Android natif depuis Angular)     │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTPS / REST API / WebSocket
                       │
┌──────────────────────▼──────────────────────────────────┐
│                API GATEWAY (Nginx)                      │
│          Load Balancing + SSL Termination               │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              BACKEND — Spring Boot 4.x                  │
│                                                         │
│  Modules métier (sous /api/v1/) :                       │
│  - auth         (JWT + Refresh Token + 2FA TOTP)        │
│  - tontine      (configuration, paramétrage)            │
│  - membre       (profils, ayants droit)                 │
│  - cotisation   (paiements, suivi, retards)             │
│  - aide         (demandes, validation, workflow)        │
│  - epargne      (comptes, mouvements, intérêts)         │
│  - pret         (demandes, amortissement, échéances)    │
│  - rapport      (PDF, Excel)                            │
│  - notification (email, SMS)                            │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              COUCHE DONNÉES                             │
│                                                         │
│  PostgreSQL 16      (données principales)               │
│  Redis 7            (sessions, cache, queues)           │
│  MinIO / S3         (fichiers : justificatifs, reçus)   │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              SERVICES TIERS                             │
│                                                         │
│  CinetPay / MTN MoMo / Orange Money (paiement Afrique)  │
│  Africa's Talking (SMS)                                 │
│  Mailgun / SendGrid (Email)                             │
│  Firebase (Push Notifications mobile)                   │
└─────────────────────────────────────────────────────────┘
```

---

## Architecture Frontend

### Structure du Projet

```
tontine-frontend/
├── src/
│   ├── app/
│   │   ├── core/                          (services singleton)
│   │   │   ├── services/
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── tontine.service.ts
│   │   │   │   ├── cotisation.service.ts
│   │   │   │   ├── epargne.service.ts
│   │   │   │   ├── pret.service.ts
│   │   │   │   └── notification.service.ts
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts
│   │   │   │   └── role.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── jwt.interceptor.ts
│   │   │   │   └── error.interceptor.ts
│   │   │   └── models/
│   │   │       ├── tontine.model.ts
│   │   │       ├── membre.model.ts
│   │   │       ├── cotisation.model.ts
│   │   │       ├── aide.model.ts
│   │   │       ├── epargne.model.ts
│   │   │       └── pret.model.ts
│   │   │
│   │   ├── shared/                        (composants réutilisables)
│   │   │   ├── components/
│   │   │   │   ├── header/
│   │   │   │   ├── bottom-nav/            (MOBILE FIRST)
│   │   │   │   ├── card/
│   │   │   │   ├── badge-statut/
│   │   │   │   ├── montant-display/
│   │   │   │   └── loader/
│   │   │   └── pipes/
│   │   │       ├── fcfa.pipe.ts
│   │   │       └── statut-label.pipe.ts
│   │   │
│   │   ├── features/                      (modules lazy-loaded)
│   │   │   ├── auth/
│   │   │   │   ├── login/
│   │   │   │   ├── register/
│   │   │   │   └── forgot-password/
│   │   │   ├── dashboard/
│   │   │   │   ├── dashboard-membre/
│   │   │   │   └── dashboard-admin/
│   │   │   ├── cotisations/
│   │   │   │   ├── liste-cotisations/
│   │   │   │   ├── payer-cotisation/
│   │   │   │   └── historique/
│   │   │   ├── aides/
│   │   │   │   ├── demande-aide/
│   │   │   │   ├── mes-demandes/
│   │   │   │   └── validation-aide/       (admin)
│   │   │   ├── epargne/
│   │   │   │   ├── mon-epargne/
│   │   │   │   ├── depot-retrait/
│   │   │   │   └── historique-epargne/
│   │   │   ├── prets/
│   │   │   │   ├── demande-pret/
│   │   │   │   ├── mes-prets/
│   │   │   │   ├── simulateur/
│   │   │   │   └── validation-pret/       (admin)
│   │   │   ├── membres/
│   │   │   │   ├── liste-membres/
│   │   │   │   └── profil-membre/
│   │   │   └── administration/
│   │   │       ├── config-tontine/
│   │   │       ├── config-aides/
│   │   │       ├── fond-developpement/
│   │   │       └── rapports/
│   │   │
│   │   ├── app.routes.ts
│   │   ├── app.component.ts
│   │   └── app.config.ts
│   │
│   ├── assets/
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.prod.ts
│   └── styles/
│       ├── _variables.scss
│       ├── _mixins.scss
│       └── styles.scss
│
├── capacitor.config.ts
├── angular.json
└── package.json
```

---

## Architecture Backend — Spring Boot 4.x (Java 21)

### Choix d'Architecture : Domain-Driven Design (DDD)

La structure du projet suit une approche **DDD** avec séparation stricte des couches :

- **`domain/`** — entités JPA et repositories (le cœur métier, sans dépendance framework)
- **`api/`** — controllers REST, services applicatifs, DTOs (la surface exposée)
- **`infrastructure/`** — configuration technique : sécurité, migrations, CORS (les détails techniques)

### Structure du Projet

```
tontinepro-backend/
├── src/
│   ├── main/
│   │   ├── java/com/tontinepro/tontinepro_backend/
│   │   │   ├── TontineproBackendApplication.java
│   │   │   │
│   │   │   ├── domain/                        (entités JPA + repositories)
│   │   │   │   ├── user/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── RefreshToken.java
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   └── RefreshTokenRepository.java
│   │   │   │   ├── tontine/
│   │   │   │   │   └── Tontine.java
│   │   │   │   ├── membre/
│   │   │   │   │   ├── Membre.java
│   │   │   │   │   └── AyantDroit.java
│   │   │   │   ├── cotisation/
│   │   │   │   │   └── Cotisation.java
│   │   │   │   ├── aide/
│   │   │   │   │   └── Aide.java
│   │   │   │   ├── epargne/
│   │   │   │   │   ├── CompteEpargne.java
│   │   │   │   │   └── MouvementEpargne.java
│   │   │   │   └── pret/
│   │   │   │       ├── Pret.java
│   │   │   │       └── EcheancePret.java
│   │   │   │
│   │   │   ├── api/                           (controllers, services, DTOs)
│   │   │   │   ├── auth/
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   └── dto/
│   │   │   │   │       ├── RegisterRequest.java
│   │   │   │   │       ├── LoginRequest.java
│   │   │   │   │       ├── RefreshRequest.java
│   │   │   │   │       └── AuthResponse.java
│   │   │   │   ├── tontine/
│   │   │   │   ├── membre/
│   │   │   │   ├── cotisation/
│   │   │   │   ├── aide/
│   │   │   │   ├── epargne/
│   │   │   │   ├── pret/
│   │   │   │   ├── rapport/
│   │   │   │   └── common/
│   │   │   │       └── GlobalExceptionHandler.java
│   │   │   │
│   │   │   └── infrastructure/                (configuration technique)
│   │   │       ├── security/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── JwtService.java
│   │   │       │   ├── JwtProperties.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   └── UserDetailsServiceImpl.java
│   │   │       └── config/
│   │   │           ├── FlywayConfig.java
│   │   │           └── CorsConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── migration/
│   │               └── V1__schema_initial.sql
│   │
│   └── test/
│       └── java/com/tontinepro/tontinepro_backend/
│           └── TontineproBackendApplicationTests.java  (Testcontainers)
│
├── docker-compose.yml
├── pom.xml
└── ARCHITECTURE.md
```

### Points Forts de l'Architecture

- **DDD :** Séparation stricte domaine / application / infrastructure
- **Spring Security 7 :** Stateless JWT, CORS configuré, RBAC (ADMIN/MEMBRE/INVITE)
- **Flyway 11 :** Migrations versionnées, `ddl-auto: none` (Flyway = seule source de vérité)
- **Testcontainers :** Tests d'intégration avec PostgreSQL + Redis réels
- **Gestion des erreurs :** RFC 9457 `ProblemDetail` via `@RestControllerAdvice`

---

## Contrat API

### Authentification

| Méthode | Endpoint                   | Description                     | Accès        |
| ------- | -------------------------- | ------------------------------- | ------------ |
| `POST`  | `/api/v1/auth/register`    | Inscription nouvel utilisateur  | Public       |
| `POST`  | `/api/v1/auth/login`       | Connexion utilisateur           | Public       |
| `POST`  | `/api/v1/auth/refresh`     | Rafraîchir le token d'accès     | Authentifié  |
| `POST`  | `/api/v1/auth/logout`      | Déconnexion utilisateur         | Authentifié  |

### Tontines

| Méthode | Endpoint                       | Description                        | Accès        |
| ------- | ------------------------------ | ---------------------------------- | ------------ |
| `POST`  | `/api/v1/tontines`             | Créer une tontine                  | Admin        |
| `GET`   | `/api/v1/tontines/{id}`        | Détails d'une tontine              | Authentifié  |
| `PATCH` | `/api/v1/tontines/{id}/config` | Mettre à jour la configuration     | Admin        |

### Membres

| Méthode | Endpoint                              | Description                     | Accès        |
| ------- | ------------------------------------- | ------------------------------- | ------------ |
| `GET`   | `/api/v1/membres`                     | Lister tous les membres         | Authentifié  |
| `GET`   | `/api/v1/membres/{id}`                | Détails d'un membre             | Authentifié  |
| `PATCH` | `/api/v1/membres/{id}`                | Mettre à jour le profil         | Membre/Admin |
| `POST`  | `/api/v1/membres/{id}/ayants-droit`   | Ajouter un bénéficiaire         | Membre/Admin |

### Cotisations

| Méthode | Endpoint                                   | Description                     | Accès        |
| ------- | ------------------------------------------ | ------------------------------- | ------------ |
| `GET`   | `/api/v1/cotisations?mois=&annee=`         | Lister les cotisations          | Authentifié  |
| `POST`  | `/api/v1/cotisations/{id}/payer`           | Payer une cotisation            | Membre       |
| `GET`   | `/api/v1/cotisations/retards`              | Cotisations en retard           | Admin        |

### Aides Mutuelles

| Méthode | Endpoint                                    | Description                  | Accès        |
| ------- | ------------------------------------------- | ---------------------------- | ------------ |
| `POST`  | `/api/v1/aides/demandes`                    | Soumettre une demande        | Membre       |
| `GET`   | `/api/v1/aides/demandes`                    | Lister les demandes          | Authentifié  |
| `PATCH` | `/api/v1/aides/demandes/{id}/valider`       | Approuver une demande        | Admin        |
| `PATCH` | `/api/v1/aides/demandes/{id}/rejeter`       | Rejeter une demande          | Admin        |

### Épargne

| Méthode | Endpoint                                  | Description                            | Accès      |
| ------- | ----------------------------------------- | -------------------------------------- | ---------- |
| `GET`   | `/api/v1/epargne/mon-compte`              | Compte d'épargne personnel             | Membre     |
| `POST`  | `/api/v1/epargne/depot`                   | Effectuer un dépôt                     | Membre     |
| `POST`  | `/api/v1/epargne/retrait`                 | Effectuer un retrait                   | Membre     |
| `GET`   | `/api/v1/epargne/historique`              | Historique des transactions            | Membre     |
| `POST`  | `/api/v1/epargne/distribuer-interets`     | Distribuer les intérêts                | Admin/Cron |

### Prêts

| Méthode | Endpoint                                          | Description                    | Accès  |
| ------- | ------------------------------------------------- | ------------------------------ | ------ |
| `POST`  | `/api/v1/prets/demande`                           | Soumettre une demande de prêt  | Membre |
| `GET`   | `/api/v1/prets/simulation?montant=&duree=`        | Simuler un prêt                | Membre |
| `GET`   | `/api/v1/prets/mes-prets`                         | Mes prêts                      | Membre |
| `PATCH` | `/api/v1/prets/{id}/valider`                      | Approuver un prêt              | Admin  |
| `POST`  | `/api/v1/prets/{id}/rembourser`                   | Effectuer un remboursement     | Membre |

### Rapports

| Méthode | Endpoint                                      | Description          | Accès | Format |
| ------- | --------------------------------------------- | -------------------- | ----- | ------ |
| `GET`   | `/api/v1/rapports/mensuel?mois=&annee=`       | Rapport mensuel      | Admin | PDF    |
| `GET`   | `/api/v1/rapports/cotisations`                | Rapport cotisations  | Admin | Excel  |
| `GET`   | `/api/v1/rapports/financier`                  | Rapport financier    | Admin | PDF    |

---

## Architecture de Sécurité

### Flux d'Authentification

1. **Connexion :** Validation des identifiants (BCrypt) → `UserDetailsServiceImpl`
2. **Génération :** Access token JWT (15 min) + refresh token UUID brut (7 jours)
3. **Stockage :** Refresh token stocké en DB sous forme de hash SHA-256 (jamais le token brut)
4. **Requêtes API :** `Authorization: Bearer <access_token>`
5. **Rafraîchissement :** Échange refresh token → nouveau access token (rotation avec révocation)
6. **Logout :** Révocation de tous les refresh tokens de l'utilisateur
7. **2FA (à venir) :** TOTP optionnel

### Modèle d'Autorisation RBAC

| Rôle    | Accès                                      |
| ------- | ------------------------------------------ |
| ADMIN   | Accès complet au système                  |
| MEMBRE  | Limité aux données et actions personnelles |
| INVITE  | Endpoints publics uniquement               |

### Bonnes Pratiques de Sécurité

- [x] Hachage des mots de passe BCrypt
- [x] Vérification de la signature JWT (HMAC-SHA256)
- [x] CORS configuré (origines autorisées paramétrables)
- [x] Stateless : aucune session serveur
- [x] Refresh token révocable (stocké hashé en DB)
- [x] Validation des entrées (`@Valid` + Bean Validation)
- [x] Réponses d'erreur standardisées RFC 9457 (`ProblemDetail`)
- [ ] Rate limiting (à implémenter)
- [ ] Protection XSS headers (à implémenter via Spring Security)

---

## Conception de la Couche de Données

### Schéma PostgreSQL — 11 Tables (V1)

| Table               | Description                          |
| ------------------- | ------------------------------------ |
| `users`             | Comptes utilisateurs + auth          |
| `refresh_tokens`    | Tokens de rafraîchissement (hashés)  |
| `tontines`          | Configuration des tontines           |
| `membres`           | Profils métier liés à un user        |
| `ayants_droit`      | Bénéficiaires des membres            |
| `cotisations`       | Cotisations mensuelles               |
| `aides`             | Demandes d'aide mutuelle             |
| `comptes_epargne`   | Comptes d'épargne (1 par membre)     |
| `mouvements_epargne`| Transactions d'épargne               |
| `prets`             | Dossiers de prêt                     |
| `echeances_pret`    | Échéancier de remboursement          |

### Utilisation de Redis

- **Cache :** Données fréquentes (config tontine, listes membres)
- **Sessions :** TTL configurable
- **File d'attente :** Notifications, rapports en arrière-plan
- **Rate limiting :** Compteurs de requêtes par IP/utilisateur

### Stockage de Fichiers (MinIO/S3)

Organisation des buckets :
- `/justificatifs/aides/`
- `/reçus/cotisations/`
- `/rapports/mensuels/`

---

## Configuration des Environnements

### Dev (profil `dev`)

```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/tontinepro_db
  username: tontinepro_user
  password: tontinepro_pass
redis:
  host: localhost
  port: 6379
```

Docker Compose local : PostgreSQL 16 + Redis 7 + pgAdmin 4

### Prod (profil `prod`)

Toutes les valeurs sensibles via variables d'environnement :
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PASSWORD`, `JWT_SECRET`

`flyway.clean-disabled: true` — protection contre effacement accidentel.

---

## Stratégie de Déploiement

### Conteneurisation

- **Docker :** Builds multi-étapes pour frontend et backend
- **Docker Compose :** Environnement de développement local
- **Kubernetes (Production) :** Orchestration pour la scalabilité

### Pipeline CI/CD

1. Push code → build + tests unitaires
2. Tests d'intégration (Testcontainers)
3. Déploiement staging
4. Déploiement production

---

## Surveillance & Observabilité

- **Logs :** Format structuré, niveaux configurables par profil
- **Métriques applicatives :** Taux de requêtes, temps de réponse, taux d'erreur
- **Métriques métier :** Membres actifs, taux de cotisation, défauts de prêt
- **Alertes :** Panne système, défaillances DB, taux d'erreur élevé

---

## Stratégie Mobile

### Progressive Web App (PWA)

- **Support Hors Ligne :** Service workers
- **Expérience Type Application :** Ajout à l'écran d'accueil, notifications push
- **Performance :** Lazy loading, code splitting

### Intégration Capacitor.js

- **Builds Natifs :** iOS et Android depuis la codebase Angular
- **Fonctionnalités Natives :** Caméra, biométrie, stockage local
- **Distribution :** App Store et Play Store

---

## Conclusion

Cette architecture fournit une base **scalable, maintenable et sécurisée** pour TontinePro. L'approche mobile-first garantit une expérience utilisateur optimale, tandis que la conception DDD du backend permet une expansion et une maintenance faciles des fonctionnalités.

**Points Forts :**

- [x] Pile technologique moderne (Spring Boot 4 / Java 21 / Angular)
- [x] DDD — séparation stricte domaine / application / infrastructure
- [x] Mobile-first (PWA + Capacitor.js)
- [x] Contrat API versionné (`/api/v1/`)
- [x] Sécurité robuste (JWT stateless, refresh token révocable, BCrypt)
- [x] Migrations versionnées (Flyway — schéma immuable en prod)
- [x] Tests d'intégration réels (Testcontainers)
- [x] Stratégie de déploiement scalable

---

*Version : 2.0 — Mise à jour suite aux choix d'implémentation*
*Dernière mise à jour : Mai 2026*
