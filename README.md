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

# 2. Lancer toute la plateforme (build + run)
docker compose up --build
```

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

---

## Diagnostic des téléchargements Maven dans Docker

Les services Java utilisent Spring Boot 3.3.5, Spring Cloud 2023.0.3 et Java 17.
Un `ping` en échec ne suffit pas à diagnostiquer Maven Central, car le trafic ICMP
peut être bloqué. Tester plutôt HTTPS et Maven depuis un conteneur :

```bash
docker run --rm alpine wget -S -O - https://repo.maven.apache.org/maven2/
docker run --rm maven:3.9.9-eclipse-temurin-17 mvn -version
docker run --rm maven:3.9.9-eclipse-temurin-17 mvn dependency:get -Dartifact=org.springframework.boot:spring-boot-starter-parent:3.3.5:pom
```

Si `wget` ne parvient pas à résoudre `repo.maven.apache.org`, configurer le moteur
Docker (Docker Desktop > Settings > Docker Engine), puis redémarrer Docker Desktop :

```json
{
  "dns": ["8.8.8.8", "1.1.1.1"]
}
```

Cette configuration appartient au moteur Docker de la machine et ne doit pas
être ajoutée au `docker-compose.yml` du projet.

Si le nom est résolu mais que la connexion au port 443 est refusée ou déclarée
inaccessible, le DNS fonctionne déjà : vérifier le pare-feu, le VPN, le proxy
d'entreprise et la configuration proxy de Docker Desktop. Autoriser au minimum
les connexions HTTPS sortantes vers `repo.maven.apache.org:443`.

Dans ce projet, les builds Docker Maven utilisent aussi le miroir Central
`https://repo1.maven.org/maven2` via `.mvn/settings-docker.xml`. Les Dockerfile
activent un cache BuildKit Maven partagé et des reprises automatiques afin que les
huit services ne retéléchargent pas toutes les dépendances. Le premier build peut
rester long sur une connexion lente ; les suivants réutilisent le cache.

Pour valider progressivement les images critiques :

```bash
docker compose build transaction-service
docker compose build auth-service
docker compose build gateway-service
docker compose up --build
docker compose ps
```
