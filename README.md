# Plateforme Bancaire Distribuée — INF462

Plateforme financière distribuée en **microservices** permettant à plusieurs
opérateurs financiers de collaborer dans un même écosystème (gestion des clients,
comptes, transactions, prêts, notifications, documents/IA), avec une interface
homogène, sécurisée et scalable.

> TP INF462 — Université de Yaoundé I · **Présentation finale : 22 juin 2026**

---

## Architecture

```
                         ┌──────────────┐
            Frontend ───▶│ API Gateway  │  (Spring Cloud Gateway, :8080)
                         └──────┬───────┘
                                │  routage lb:// + résilience
        ┌──────────┬──────────┬─┴────────┬───────────┬─────────────┐
        ▼          ▼          ▼          ▼           ▼             ▼
   customer    account    transaction   loan    notification   ai-document
   (Java)      (Java)      (Java)       (Java)   (Node.js)      (Python/OCR)
        │          │          │          │
        └──────────┴────┬─────┴──────────┘
                        ▼
              Discovery (Eureka, :8761)   ◀── enregistrement de tous les services

   Communication asynchrone : RabbitMQ (événements métier)
   Persistance : PostgreSQL (1 base par service)
```

### Stack technique
| Couche | Technologie |
|--------|-------------|
| Services cœur | Java 17 / Spring Boot 4 / Spring Cloud |
| Découverte | Netflix Eureka |
| Passerelle | Spring Cloud Gateway |
| Notifications | Node.js |
| OCR / IA | Python / FastAPI / Tesseract |
| Base de données | PostgreSQL (database per service) |
| Messagerie | RabbitMQ |
| Conteneurisation | Docker / Docker Compose |
| Orchestration | Kubernetes _(à venir)_ |
| CI/CD | _(à venir — GitHub Actions)_ |

---

## Démarrage rapide (développement local)

**Prérequis** : Docker + Docker Compose, JDK 17, Maven (ou le wrapper `./mvnw`).

```bash
# 1. Configurer les variables d'environnement
cp .env.example .env        # puis adapter les mots de passe

# 2. Construire proprement les .jar des services Java
chmod +x scripts/build-java-services.sh
./scripts/build-java-services.sh

# 3. Lancer toute la plateforme (build + run)
docker compose up --build -d
```

Les Dockerfiles des services Java copient les fichiers `target/*.jar` existants.
Il faut donc reconstruire les `.jar` avant `docker compose up --build`, surtout
apres une modification Java ou si un service demarre avec une ancienne image.
Le script utilise `clean package -Dmaven.test.skip=true` pour eviter les artefacts
obsoletes et ignorer aussi la compilation des tests locaux non alignes.

| Service | URL |
|---------|-----|
| Eureka (découverte) | http://localhost:8761 |
| API Gateway | http://localhost:8080 |
| customer-service | http://localhost:8081 |
| account-service | http://localhost:8082 |
| transaction-service | http://localhost:8083 |
| loan-service | http://localhost:8084 |
| RabbitMQ (console) | http://localhost:15672 |

### Lancer un seul service (sans Docker)
```bash
cd microservices-backend/<service>
./mvnw spring-boot:run
```

---

## Structure du dépôt

```
.
├── microservices-backend/     # Services Java/Spring (discovery, gateway, customer, account, transaction, loan)
├── notification-service/      # Service de notifications (Node.js)
├── ai-document-service/       # Service OCR + IA (Python/FastAPI)
├── frontend-app/              # Interface utilisateur (à développer)
├── infra/                     # Scripts d'infrastructure (init BDD, etc.)
├── docs/                      # Cahier des charges, analyse DDD, UML, rapport
├── docker-compose.yml         # Orchestration locale
└── .env.example               # Modèle de variables d'environnement
```

---

## Suivi des livrables (TP)

| # | Livrable | Emplacement | État |
|---|----------|-------------|------|
| 1 | Cahier des charges fonctionnel et non fonctionnel | [docs/02-cahier-des-charges.md](docs/02-cahier-des-charges.md) | 🟡 squelette |
| 2 | Étude DDD complète | [docs/01-analyse-ddd.md](docs/01-analyse-ddd.md) | 🟡 squelette |
| 3 | Proposition d'architecture microservices justifiée | [docs/03-modele-domaine-et-plan.md](docs/03-modele-domaine-et-plan.md) | 🟡 amorcé |
| 4 | Diagrammes UML (cas d'usage, classes, MCD/MLD) | [docs/03-modele-domaine-et-plan.md](docs/03-modele-domaine-et-plan.md) | 🟢 amorcé |
| 5 | Code source complet (GitHub) | ce dépôt | 🟡 échafaudage |
| 6 | Documentation technique et des API | `docs/` + Swagger | 🔴 à faire |
| 7 | Fichiers de conteneurisation Docker | `*/Dockerfile`, `docker-compose.yml` | 🟢 amorcé |
| 8 | Fichiers de déploiement Kubernetes | `k8s/` | 🔴 à faire |
| 9 | Documentation du processus CI/CD | `.github/workflows/`, `docs/` | 🔴 à faire |
| 10 | Démonstration fonctionnelle en production | — | 🔴 à faire |
| 11 | Rapport technique | `docs/` | 🔴 à faire |

> 🟢 fait · 🟡 en cours / squelette · 🔴 à faire

---

## Démarche de travail

Le **modèle de données complet** (entités, attributs, relations), les **diagrammes**
(cas d'usage, classes, MCD, MLD), le **plan de démarrage** (par quoi commencer) et
la **répartition anti-conflits** de l'équipe sont détaillés dans
👉 **[docs/03-modele-domaine-et-plan.md](docs/03-modele-domaine-et-plan.md)**.

**Principe clé : se mettre d'accord sur les contrats (API + événements) et le
modèle de données AVANT d'écrire la logique métier.** On démarre par
`customer-service` (modèle de référence) + `auth-service`.

## Équipe
_(noms des membres et répartition par bounded context à compléter)_
