# Fiche Technique — Plateforme Bancaire Distribuée

> TP INF462 — Université de Yaoundé I · Présentation : 22 juin 2026

---

## Table des matières

1. [Présentation du projet](#1-présentation-du-projet)
2. [Stack technologique globale](#2-stack-technologique-globale)
3. [Architecture générale](#3-architecture-générale)
4. [Services en détail](#4-services-en-détail)
   - 4.1 [config-service](#41-config-service)
   - 4.2 [discovery-service](#42-discovery-service)
   - 4.3 [gateway-service](#43-gateway-service)
   - 4.4 [auth-service](#44-auth-service)
   - 4.5 [customer-service](#45-customer-service)
   - 4.6 [account-service](#46-account-service)
   - 4.7 [transaction-service](#47-transaction-service)
   - 4.8 [loan-service](#48-loan-service)
   - 4.9 [ai-document-service](#49-ai-document-service)
   - 4.10 [notification-service](#410-notification-service)
   - 4.11 [frontend-app](#411-frontend-app)
5. [Communication inter-services](#5-communication-inter-services)
6. [Sécurité](#6-sécurité)
7. [Modèle de données](#7-modèle-de-données)
8. [Conteneurisation Docker](#8-conteneurisation-docker)
9. [Déploiement Kubernetes](#9-déploiement-kubernetes)
10. [Pipeline CI/CD](#10-pipeline-cicd)
11. [Observabilité](#11-observabilité)
12. [Configuration et variables d'environnement](#12-configuration-et-variables-denvironnement)
13. [URLs et ports](#13-urls-et-ports)
14. [Démarrage rapide](#14-démarrage-rapide)

---

## 1. Présentation du projet

Plateforme financière distribuée construite en **microservices polyglots** permettant à plusieurs opérateurs financiers (banques, microfinances, opérateurs mobiles) de collaborer dans un même écosystème. Elle couvre la gestion des clients, des comptes, des transactions, des prêts, des notifications, et intègre un service d'IA/OCR pour l'analyse automatique de documents.

**Contexte** : TP académique INF462, démontrant les patterns cloud-native (service discovery, API gateway, circuit breaker, event-driven architecture), le polyglottisme technologique (Java, Node.js, Python), et les pratiques DevOps (Docker, Kubernetes, CI/CD, observabilité).

**Acteurs du système :**

| Acteur | Rôle |
|--------|------|
| Client | Gère ses comptes, effectue des opérations, demande des prêts |
| Opérateur financier | Définit ses règles métier (plafonds, commissions, validations) |
| Administrateur | Supervise la plateforme, gère les opérateurs et les utilisateurs |
| Système IA/OCR | Extrait et vérifie automatiquement les informations des documents |

---

## 2. Stack technologique globale

| Couche | Technologie | Version |
|--------|-------------|---------|
| Services Java | Spring Boot | 4.0.6 |
| Framework cloud | Spring Cloud | 2025.1.1 |
| Découverte de services | Netflix Eureka | Spring Cloud |
| API Gateway | Spring Cloud Gateway (WebFlux/réactif) | Spring Cloud |
| Sécurité | Spring Security + JJWT | 0.12.6 |
| Auth Google | google-api-client | 2.7.0 |
| ORM Java | Spring Data JPA (Hibernate) | Spring Boot |
| Base de données principale | PostgreSQL | 16-alpine |
| Broker de messages | RabbitMQ | 3-management-alpine |
| Event streaming | Apache Kafka (transaction-service) | Spring Boot |
| Circuit breaker | Resilience4j | Spring Cloud |
| Service mesh interne | Spring Cloud LoadBalancer + OpenFeign | Spring Cloud |
| Métriques | Micrometer + Prometheus | Spring Boot Actuator |
| Dashboards | Grafana | latest |
| Documentation API Java | SpringDoc OpenAPI (Swagger UI) | 3.0.0 |
| Service Node.js | Node.js + Express | 20 / 4.19.2 |
| Messagerie Node | amqplib | 0.10.4 |
| Email | Nodemailer | 6.9.13 |
| Service Python | FastAPI + Uvicorn | 0.111.0 / 0.30.1 |
| OCR | Tesseract OCR (FR + EN) | système |
| Traitement image | OpenCV + pytesseract | 4.9.0.80 / 0.3.10 |
| ORM Python | SQLAlchemy | 2.0.31 |
| Base Python | SQLite | embarquée |
| Validation Python | Pydantic | 2.7.4 |
| Tests Python | pytest | 8.2.2 |
| Frontend | Angular | 21.2.0 |
| Langage frontend | TypeScript | 5.9.2 |
| Réactivité frontend | RxJS | 7.8.0 |
| Serveur frontend | Nginx (SPA proxy) | alpine |
| Tests frontend | Vitest | 4.0.8 |
| Conteneurisation | Docker / Docker Compose | — |
| Orchestration | Kubernetes (minikube) | — |
| CI/CD | GitHub Actions | — |
| Build Java | Maven (wrapper mvnw) | 3.9.9 |

---

## 3. Architecture générale

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │                     CLIENTS / FRONTEND                              │
  │              Angular 21 (Nginx, port 4200)                          │
  └──────────────────────────┬──────────────────────────────────────────┘
                             │  HTTP  /api/*
                             ▼
  ┌──────────────────────────────────────────────────────────────────────┐
  │                      API GATEWAY  (:8080)                            │
  │        Spring Cloud Gateway WebFlux — vérification JWT              │
  │        Routage lb://service-name → Eureka                            │
  └───┬──────────┬──────────┬──────────┬──────────┬──────────┬──────────┘
      │          │          │          │          │          │
      ▼          ▼          ▼          ▼          ▼          ▼
  auth       customer   account  transaction   loan    ai-document
 (:8085)     (:8081)   (:8082)   (:8083)    (:8084)    (:8001)
      │          │          │          │          │
      └──────────┴──────────┴────┬─────┴──────────┘
                                 │
                    ┌────────────▼────────────┐
                    │  EUREKA Discovery       │
                    │       (:8761)           │
                    └─────────────────────────┘

  Communication asynchrone (événements) :
  transaction / loan / customer / account
          │
          ▼  RabbitMQ exchange: bank.events (topic)
          │
  notification-service (:3000)  ←  consomme tous les événements métier

  Infrastructure transversale :
  ├── PostgreSQL (:5432)  — 1 base par service (database per service)
  ├── Config-service (:8888) — configuration centralisée Spring Cloud Config
  ├── Prometheus (:9090) — collecte des métriques Micrometer/Actuator
  └── Grafana (:3001) — dashboards de supervision
```

### Principes architecturaux

- **Database per service** : chaque microservice possède sa propre base PostgreSQL, aucune jointure SQL inter-services.
- **API Gateway unique** : seul point d'entrée public, validation JWT centralisée.
- **Service Discovery** : Eureka, aucune adresse IP en dur, routing via `lb://service-name`.
- **Eventdriven** : RabbitMQ découple les producteurs d'événements (transaction, loan…) des consommateurs (notification, account, loan).
- **Circuit Breaker** : Resilience4j protège les appels synchrones critiques (transaction → account).
- **Configuration centralisée** : Spring Cloud Config Server (`config-service`) distribue la configuration à tous les services Java.
- **Polyglottisme justifié** : Java pour la logique financière (fiabilité, JPA, Spring Security), Python pour l'IA/OCR (ecosystème NumPy/OpenCV/Tesseract), Node.js pour les notifications réactives/event-driven.

---

## 4. Services en détail

### 4.1 config-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Java 17 / Spring Boot 4.0.6 |
| Port | 8888 |
| Rôle | Serveur de configuration centralisée (Spring Cloud Config) |
| Base de données | Aucune |

**Fonctionnement** : centralise toutes les propriétés des services Java. Au démarrage, chaque service Java contacte `http://config-service:8888` pour récupérer sa configuration (datasource, eureka URL, clés secrètes). Les fichiers de configuration résident dans `microservices-backend/cloud-conf/`.

---

### 4.2 discovery-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Java 17 / Spring Boot 4.0.6 / Netflix Eureka Server |
| Port | 8761 |
| Rôle | Annuaire de services — enregistrement et découverte |
| Base de données | Aucune (registre en mémoire) |

**Fonctionnement** : chaque microservice s'y enregistre au démarrage avec son nom logique (`AUTH-SERVICE`, `CUSTOMER-SERVICE`…). La Gateway interroge Eureka pour résoudre `lb://auth-service` en adresse IP réelle. Cela permet la scalabilité horizontale sans reconfiguration.

Console web : `http://localhost:8761`

---

### 4.3 gateway-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Java 17 / Spring Cloud Gateway (WebFlux réactif) |
| Port | 8080 |
| Rôle | Point d'entrée unique + vérification JWT + routage |
| Dépendances clés | JJWT 0.12.6, Spring Cloud Eureka Client, Micrometer Prometheus |

**Fonctionnement** :

```
Requête entrante
    │
    ├─ /api/auth/** → public, laissé passer sans vérification
    │
    └─ Tout autre endpoint → vérification du header Authorization: Bearer <token>
            ├─ Token invalide/absent → 401 Unauthorized (la requête n'atteint jamais le service)
            └─ Token valide → ajout des headers X-User-Email, X-User-Roles
                           → routage lb://<service-name> via Eureka
```

**Routes configurées :**

| Chemin | Service cible | Méthode d'accès |
|--------|---------------|-----------------|
| `/api/auth/**` | `lb://auth-service` | Public |
| `/api/customers/**` | `lb://customer-service` | Authentifié |
| `/api/accounts/**` | `lb://account-service` | Authentifié |
| `/api/transactions/**` | `lb://transaction-service` | Authentifié |
| `/api/loans/**` | `lb://loan-service` | Authentifié |
| `/api/v1/**` | `http://ai-document-service:8001` | Authentifié |
| `/api/notifications/**` | `lb://notification-service` | Authentifié |

---

### 4.4 auth-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Java 17 / Spring Boot 4.0.6 / Spring Security |
| Port | 8085 |
| Base de données | `bank_auth_db` (PostgreSQL) |
| Dépendances clés | JJWT 0.12.6, BCrypt, google-api-client 2.7.0, Lombok |

**Entités :**
- `Utilisateur` : id (UUID/Long), email (unique), motDePasse (haché BCrypt), telephone, statut (ACTIF/SUSPENDU), dateCreation
- `Role` : CLIENT, ADMIN, OPERATEUR (relation n..n avec Utilisateur)

**Endpoints REST :**

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| POST | `/api/auth/register` | Public | Inscription avec email + mot de passe |
| POST | `/api/auth/login` | Public | Connexion → JWT Bearer |
| POST | `/api/auth/google` | Public | Connexion Google OAuth2 (id_token) |
| GET | `/api/auth/me` | JWT | Infos utilisateur courant |

**Flux d'authentification :**
1. `register` : hachage BCrypt du mot de passe, création Utilisateur + Role CLIENT
2. `login` : vérification BCrypt → signature JWT avec `JWT_SECRET` → `{ token, type: "Bearer", expiresIn: 86400 }`
3. `google` : vérification de l'`idToken` via `GoogleIdTokenVerifier` → création/récupération utilisateur → JWT interne

**Architecture interne :**
```
AuthController → AuthService → UserRepository → PostgreSQL
                     └─ JwtService (signe/vérifie)
                     └─ GoogleTokenVerifier
                     └─ PasswordEncoder (BCrypt)
```

---

### 4.5 customer-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Java 17 / Spring Boot 4.0.6 |
| Port | 8081 |
| Base de données | `bank_customer_db` (PostgreSQL) |
| Dépendances clés | Spring Data JPA, Lombok, Eureka Client |

**Entités :**
- `Client` : id, utilisateurId (réf auth), operateurId (réf), nom, prenom, dateNaissance, email, telephone, numeroIdentite, typePiece (CNI/PASSEPORT), statutKYC (EN_ATTENTE/VALIDE/REJETE), adresse (Value Object)
- `Operateur` : id, nom, type (BANQUE/MICROFINANCE/MOBILE), code (unique)

**Endpoints REST :**

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/customers` | Créer un client |
| GET | `/api/customers/{id}` | Détails client |
| GET | `/api/customers` | Liste paginée |
| PUT | `/api/customers/{id}` | Modifier un client |
| PATCH | `/api/customers/{id}/kyc` | Mettre à jour le statut KYC |
| GET | `/api/customers/by-email/{email}` | Trouver par email |
| GET | `/api/operators` | Lister les opérateurs |
| POST | `/api/operators` | Créer un opérateur (admin) |

**Événements publiés (RabbitMQ) :**
- `customer.created` → payload : `{ clientId, email, nom }`
- `customer.kyc.validated` → payload : `{ clientId, statutKYC }`

---

### 4.6 account-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Java 17 / Spring Boot 4.0.6 |
| Port | 8082 |
| Base de données | `bank_account_db` (PostgreSQL) |
| Dépendances clés | Spring Data JPA, OpenFeign, Eureka Client, Lombok |

**Entité `Compte` :** id, numeroCompte (unique, format `CM-XXXX-XXXX`), clientId (réf), operateurId (réf), type (COURANT/EPARGNE), solde (Decimal), devise (XAF, EUR…), plafondJournalier, decouvertAutorise, statut (ACTIF/BLOQUE/CLOTURE), dateOuverture.

**Endpoints REST :**

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/accounts` | Ouvrir un compte |
| GET | `/api/accounts/{id}` | Détails du compte |
| GET | `/api/accounts?clientId={id}` | Comptes d'un client |
| GET | `/api/accounts/{id}/balance` | Solde courant |
| POST | `/api/accounts/{id}/credit` | Créditer (usage interne) |
| POST | `/api/accounts/{id}/debit` | Débiter (usage interne — `409` si solde insuffisant) |

**Événements publiés :**
- `account.created` → payload : `{ compteId, clientId, numeroCompte }`

**Événements consommés :**
- `loan.approved` → verse les fonds sur le compte du client

---

### 4.7 transaction-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Java 17 / Spring Boot 4.0.6 |
| Port | 8083 |
| Base de données | `bank_transaction_db` (PostgreSQL) |
| Dépendances clés | Spring AMQP (RabbitMQ), Spring Kafka, Resilience4j, OpenFeign, SpringDoc OpenAPI 3.0.0 |

**Entité `Transaction` :** id, reference (unique, format `TX-YYYY-XXXXXX`), type (DEPOT/RETRAIT/TRANSFERT), montant, devise, compteSourceId, compteDestId, operateurSourceId, operateurDestId, commission, statut (INITIEE/VALIDEE/REJETEE), motif, dateOperation.

**Endpoints REST :**

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/transactions/deposit` | Dépôt (crédit account-service) |
| POST | `/api/transactions/withdraw` | Retrait (débit account-service) |
| POST | `/api/transactions/transfer` | Transfert intra/inter-opérateurs |
| GET | `/api/transactions/{id}` | Détails |
| GET | `/api/transactions?accountId={id}` | Historique d'un compte |

**Résilience :** appels vers `account-service` (credit/debit) protégés par un **circuit breaker Resilience4j**. Si `account-service` est indisponible, la transaction passe en statut `REJETEE` plutôt que de bloquer indéfiniment.

**Événements publiés (RabbitMQ, exchange `banking.events`) :**
- `transaction.completed` → payload : `{ transactionId, type, compteSourceId, compteDestId, montant, devise }`
- `transaction.failed` → payload : `{ transactionId, raison }`

Documentation Swagger auto-générée : `http://localhost:8083/swagger-ui.html`

---

### 4.8 loan-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Java 17 / Spring Boot 4.0.6 |
| Port | 8084 |
| Base de données | `bank_loan_db` (PostgreSQL) |
| Dépendances clés | Spring Kafka, OpenFeign, Spring Data JPA, Lombok |
| Profil Docker | `application-docker.yml` (activé via `SPRING_PROFILES_ACTIVE=docker`) |

**Entités :**
- `DemandePret` : id, clientId, montantDemande, dureeMois, motif, scoreRisque, statut (SOUMISE/EN_ANALYSE/APPROUVEE/REJETEE), dateSoumission
- `Pret` : id, demandeId, clientId, compteId, montantAccorde, tauxInteret, dureeMois, capitalRestant, statut (ACTIF/SOLDE/EN_DEFAUT), dateDeblocage
- `Echeance` : id, pretId, numero, dateEcheance, montantCapital, montantInteret, montantTotal, statut (A_PAYER/PAYEE/EN_RETARD)
- `Remboursement` : id, echeanceId, montant, datePaiement, moyenPaiement

**Endpoints REST :**

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/loans/applications` | Soumettre une demande de prêt |
| GET | `/api/loans/applications/{id}` | Détails d'une demande |
| POST | `/api/loans/applications/{id}/decision` | Approuver / Rejeter (opérateur) |
| GET | `/api/loans/{id}` | Détails d'un prêt |
| GET | `/api/loans/{id}/schedule` | Échéancier (mensualités constantes) |
| POST | `/api/loans/{id}/repay` | Rembourser une échéance |

**Génération de l'échéancier :** formule des annuités constantes — `M = P × [r(1+r)^n] / [(1+r)^n - 1]` où P = capital, r = taux mensuel, n = durée en mois.

**Événements publiés :**
- `loan.approved` → `{ pretId, clientId, compteId, montantAccorde }`
- `loan.rejected` → `{ demandeId, clientId, motif }`
- `loan.installment.overdue` → `{ pretId, echeanceNumero, montant }`

**Événements consommés :**
- `customer.kyc.validated` → débloque l'analyse du dossier
- `document.verified` → alimente le score de risque

---

### 4.9 ai-document-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Python 3.12 / FastAPI 0.111.0 / Uvicorn 0.30.1 |
| Port | 8001 |
| Base de données | SQLite (`storage/app.db`) via SQLAlchemy 2.0.31 |
| OCR | Tesseract OCR (langues FR + EN installées dans le conteneur) |
| Traitement image | OpenCV 4.9.0.80 + pytesseract 0.3.10 |
| Validation | Pydantic 2.7.4 |

**Architecture interne (Clean Architecture) :**
```
Route (FastAPI)
    → Service (logique OCR + image)
        → ImagePreprocessingService (OpenCV : niveaux de gris, réduction bruit, seuillage)
        → Tesseract OCR (extraction texte)
    → Repository
        → SQLAlchemy → SQLite
```

**Pipeline OCR :**
1. Réception de l'image (PNG/JPG/JPEG) via `multipart/form-data`
2. Prétraitement OpenCV : conversion niveaux de gris → réduction du bruit (filtre gaussien) → seuillage adaptatif
3. Extraction texte par Tesseract (langues configurées FR/EN)
4. Calcul du score de confiance
5. Persistance dans `document_analyses` (SQLite)
6. Réponse JSON normalisée `{ status, message, data }`

**Endpoints REST :**

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/v1/health/` | État du service |
| POST | `/api/v1/ocr/extract` | Upload image → extraction OCR |
| GET | `/api/v1/ocr/history` | Historique des analyses |
| GET | `/api/v1/ocr/history/{id}` | Détail d'une analyse |
| POST | `/api/v1/analysis/` | Analyse statistique d'une liste de nombres |

**Codes d'erreur :**
- `400` — image invalide ou extension non supportée
- `422` — fichier absent dans la requête
- `503` — Tesseract introuvable/indisponible

**Formats de réponse :**
```json
// Succès
{ "status": "success", "message": "OCR effectué avec succès",
  "data": { "id": 1, "texte_extrait": "...", "score_confiance": 0.94 } }

// Erreur
{ "status": "error", "message": "Extension de fichier non supportée",
  "errors": { "allowed_extensions": [".jpeg", ".jpg", ".png"] } }
```

**Documentation interactive** : `http://localhost:8001/docs` (Swagger UI FastAPI) et `http://localhost:8001/redoc` (ReDoc).

**Événements publiés :**
- `document.verified` → `{ documentId, clientId, type, scoreConfiance, donneesStructurees }`

**Stockage :**
- Base SQLite : `storage/app.db` (table `document_analyses`)
- Uploads : `app/storage/uploads/` (volume Docker monté)

---

### 4.10 notification-service

| Propriété | Valeur |
|-----------|--------|
| Technologie | Node.js 20 / Express 4.19.2 |
| Port | 3000 |
| Base de données | En mémoire (50 dernières notifications, circulaire) |
| Messagerie | amqplib 0.10.4 (consommateur RabbitMQ) |
| Email | Nodemailer 6.9.13 (SMTP ou Ethereal pour les tests) |

**Fonctionnement :**
- Consomme la queue RabbitMQ `QUEUE_NOTIF` liée à l'exchange `bank.events`
- Pour chaque message reçu : parse le JSON → délègue au `transactionHandler` → envoie un email via Nodemailer → acquitte le message (`channel.ack`)
- En cas d'erreur : `channel.nack(msg, false, true)` → requeue automatique (réessai)
- Si SMTP non configuré : utilise **Ethereal** (boîte de test virtuelle), le service reste fonctionnel

**Événements consommés :**

| Événement (routing key) | Action |
|------------------------|--------|
| `customer.created` | Email de bienvenue au client |
| `customer.kyc.validated` | Notification validation KYC |
| `account.created` | Confirmation ouverture de compte |
| `transaction.completed` | Confirmation de transaction |
| `transaction.failed` | Alerte d'échec de transaction |
| `loan.approved` | Notification approbation du prêt |
| `loan.rejected` | Notification rejet du prêt |
| `loan.installment.overdue` | Rappel d'échéance impayée |
| `document.verified` | Confirmation vérification document |

**Endpoints HTTP :**

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/health` | État du service (Docker healthcheck) |
| GET | `/api/notifications` | 50 dernières notifications (consultables par le frontend) |

---

### 4.11 frontend-app

| Propriété | Valeur |
|-----------|--------|
| Technologie | Angular 21.2.0 / TypeScript 5.9.2 |
| Port | 4200 (Nginx, port interne 80) |
| Framework | Angular standalone components |
| Tests | Vitest 4.0.8 |
| Formatage | Prettier 3.8.1 |
| Serveur prod | Nginx (alpine) |

**Architecture :**
- Application SPA (Single Page Application) compilée par `ng build --configuration production`
- Servie par Nginx qui joue le rôle de proxy inverse :
  - `/api/*` → proxy vers `http://gateway-service:8080` (tous les appels API)
  - Toute autre route → `index.html` (routing Angular côté client)

**Espaces utilisateur prévus :** Client (tableau de bord, comptes, transactions, prêts, documents) · Opérateur (validation KYC, décisions prêts) · Administrateur (gestion opérateurs, supervision).

**Proxy de développement :** `proxy.conf.json` redirige `/api` vers `http://localhost:8080` lors du `ng serve` local.

---

## 5. Communication inter-services

### 5.1 Communication synchrone (REST via Eureka)

```
Service A  →  Feign Client (lb://service-b)  →  Spring Cloud LoadBalancer  →  Eureka  →  Service B
```

- La Gateway résout les routes via `lb://service-name`
- Les services Java communiquent entre eux via **OpenFeign** avec load balancing
- Exemple : `transaction-service` appelle `account-service` pour débiter/créditer

### 5.2 Communication asynchrone (RabbitMQ)

```
Producteur                    RabbitMQ                        Consommateur
─────────────                ──────────────────────────       ─────────────────
transaction-service  ──►  exchange: bank.events (topic)  ──►  notification-service
loan-service         ──►  routing key: <domaine>.<event>  ──►  account-service
customer-service     ──►  ex: "loan.approved"             ──►  loan-service
ai-document-service  ──►  ex: "transaction.completed"     ──►  customer-service
```

**Enveloppe commune des événements :**
```json
{
  "eventId": "uuid",
  "eventType": "PretApprouve",
  "occurredAt": "2026-06-06T12:00:00Z",
  "source": "loan-service",
  "data": { }
}
```

**Catalogue des événements :**

| Routing key | Producteur | Consommateurs |
|-------------|-----------|---------------|
| `customer.created` | customer | notification |
| `customer.kyc.validated` | customer | loan, notification |
| `account.created` | account | notification |
| `transaction.completed` | transaction | account, notification |
| `transaction.failed` | transaction | notification |
| `document.verified` | ai-document | customer, loan |
| `loan.approved` | loan | account, notification |
| `loan.rejected` | loan | notification |
| `loan.installment.overdue` | loan | notification |

---

## 6. Sécurité

### Flux d'authentification complet

```
1. POST /api/auth/register  { email, motDePasse }
   → auth-service hache le mot de passe (BCrypt, coût 10)
   → crée Utilisateur + Role CLIENT en base
   → retourne { id, email, roles }

2. POST /api/auth/login  { email, motDePasse }
   → vérifie BCrypt
   → signe un JWT (HMAC-SHA256) avec JWT_SECRET (256 bits min)
   → retourne { token, type: "Bearer", expiresIn: 86400 }

3. Appels authentifiés  Authorization: Bearer <token>
   → Gateway intercepte et vérifie la signature JWT (même JWT_SECRET)
   → Token valide  → route vers le service + headers X-User-Email / X-User-Roles
   → Token absent/invalide  → 401 Unauthorized (requête bloquée)

4. POST /api/auth/google  { idToken }
   → GoogleIdTokenVerifier valide le token Google
   → création/récupération de l'utilisateur en base
   → émission d'un JWT interne standard
```

### Modèle de sécurité

| Mécanisme | Détail |
|-----------|--------|
| Hachage | BCrypt (mot de passe jamais stocké en clair) |
| JWT | HMAC-SHA256, signé par auth-service, vérifié par la Gateway |
| Stateless | Aucune session serveur, JWT auto-porteur |
| Clé partagée | `JWT_SECRET` env var, même valeur dans auth-service et gateway-service |
| OAuth2 | Google Sign-In (optional, `GOOGLE_CLIENT_ID` env var) |
| Headers internes | `X-User-Email`, `X-User-Roles` injectés par la Gateway aux services aval |
| Endpoints publics | Uniquement `/api/auth/**` (register, login, google) |

---

## 7. Modèle de données

### Principe : Database per Service

Chaque microservice possède sa propre base PostgreSQL. Les relations inter-services sont des **identifiants logiques** (pas de clés étrangères SQL cross-base). La cohérence éventuelle est assurée par les événements RabbitMQ.

```sql
-- Script init (infra/postgres/init.sql)
CREATE DATABASE bank_auth_db;
CREATE DATABASE bank_customer_db;
CREATE DATABASE bank_account_db;
CREATE DATABASE bank_transaction_db;
CREATE DATABASE bank_loan_db;
-- ai-document-service utilise SQLite embarqué (storage/app.db)
```

### Schéma logique global (MLD)

```
bank_auth_db
├── UTILISATEUR(id*, email, mot_de_passe, telephone, statut, date_creation)
├── ROLE(id*, nom)
└── UTILISATEUR_ROLE(#utilisateur_id*, #role_id*)

bank_customer_db
├── OPERATEUR(id*, nom, type, code)
└── CLIENT(id*, #utilisateur_id, #operateur_id, nom, prenom, date_naissance,
          email, telephone, numero_identite, type_piece, statut_kyc,
          rue, ville, pays, code_postal, date_inscription)

bank_account_db
└── COMPTE(id*, numero_compte, #client_id, #operateur_id, type, solde, devise,
          plafond_journalier, decouvert_autorise, statut, date_ouverture)

bank_transaction_db
└── TRANSACTION(id*, reference, type, montant, devise, #compte_source_id,
               #compte_dest_id, #operateur_source_id, #operateur_dest_id,
               commission, statut, motif, date_operation)

bank_loan_db
├── DEMANDE_PRET(id*, #client_id, montant_demande, duree_mois, motif,
               score_risque, statut, date_soumission)
├── PRET(id*, #demande_id, #client_id, #compte_id, montant_accorde, taux_interet,
         duree_mois, capital_restant, statut, date_deblocage)
├── ECHEANCE(id*, #pret_id, numero, date_echeance, montant_capital,
            montant_interet, montant_total, statut)
└── REMBOURSEMENT(id*, #echeance_id, montant, date_paiement, moyen_paiement)

ai-document (SQLite)
├── DOCUMENT(id*, #client_id, type, url_fichier, statut, date_soumission)
└── DOCUMENT_ANALYSES(id*, original_filename, extracted_text, confidence_score,
                      status, created_at)
```

---

## 