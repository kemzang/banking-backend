# Intégration des services — changements à apporter

> Ce document liste ce qu'il faut ajuster pour que les contributions récentes
> (**ai-document-service** de Daryl, **config-service** de Tony) s'assemblent
> correctement avec le reste de la plateforme.

---

## A. ai-document-service (Daryl) — service OCR/IA (Python/FastAPI)

### Ce qui a été livré ✅
Un microservice FastAPI **bien structuré** (couches `routes / services / repositories /
schemas / models / core`), avec OCR via Tesseract, tests, et endpoints :
- `POST /api/v1/ocr/extract` — extraire le texte d'une image
- `GET  /api/v1/ocr/history` — historique des analyses
- `GET  /api/v1/analysis/...` — analyse de documents
- `GET  /api/v1/health/` — état du service

Code syntaxiquement correct, base **SQLite** autonome, port **8001**.

### Ce qui ne « matche » pas encore avec le reste

| # | Problème | Impact | Correction |
|---|----------|--------|------------|
| 1 | **Absent du `docker-compose.yml` racine** | Le service n'est pas lancé avec les autres | Ajouter le bloc ci-dessous |
| 2 | **Aucune route dans la Gateway** | Inaccessible via `:8080` (le point d'entrée unique) | Ajouter la route ci-dessous |
| 3 | **Ne s'enregistre pas dans Eureka** (service Python) | La Gateway ne peut pas le trouver via `lb://` | Router par **URL directe** (`http://ai-document-service:8001`), pas `lb://` |
| 4 | **Chemins `/api/v1/...`** ≠ contrat `/api/documents` | Incohérence avec [contracts](contracts/01-api-rest.md) | Mettre à jour le contrat (ou préfixer côté gateway) — voir note |
| 5 | **`Docker-compose.yml` interne** au service (doublon) | Risque de confusion / double orchestration | Le garder seulement pour le dev isolé ; en équipe, utiliser le compose racine |
| 6 | **Base SQLite** (pas `bank_document_db` Postgres) | Choix autonome, OK, mais diffère du plan initial | Acceptable (« database per service ») — à documenter |

### ✅ Correctif 1 — ajouter au `docker-compose.yml` **racine**
(remplacer le bloc commenté `# ai-document-service:` existant)
```yaml
  ai-document-service:
    build: ./ai-document-service
    container_name: bank-ai
    ports:
      - "8001:8001"
    environment:
      TESSERACT_CMD: /usr/bin/tesseract
      DATABASE_URL: sqlite:///./storage/app.db
    volumes:
      - ./ai-document-service/storage:/app/storage
      - ./ai-document-service/app/storage:/app/app/storage
    networks: [bank-net]
```

### ✅ Correctif 2 — ajouter la route dans la Gateway
Dans `gateway-service/src/main/resources/application.yml`, sous
`spring.cloud.gateway.server.webflux.routes` :
```yaml
            - id: ai-document-service
              uri: http://ai-document-service:8001   # URL directe (pas lb://, car pas dans Eureka)
              predicates:
                - Path=/api/v1/**
```
> ⚠️ Conséquence sécurité : avec notre filtre JWT, tout ce qui passe par la gateway
> (sauf `/api/auth/**`) **exige un token valide**. Donc `/api/v1/ocr/extract` via
> `:8080` nécessitera un `Authorization: Bearer ...`. C'est cohérent (seuls les
> utilisateurs connectés analysent des documents). Si on veut exposer `/api/v1/health`
> publiquement, l'ajouter à la liste `PUBLIC_PATHS` du `JwtAuthenticationFilter`.

### Note sur les chemins (#4)
Deux options, à décider en équipe :
- **(simple)** garder les chemins de Daryl (`/api/v1/...`) et **mettre à jour le contrat**.
- **(aligné)** demander à Daryl de renommer ses routes en `/api/documents/...`.
Je recommande la 1ʳᵉ (ne pas casser un service qui marche), en mettant à jour
[`contracts/01-api-rest.md`](contracts/01-api-rest.md).

---

## B. config-service (Tony) — configuration centralisée (rappel, encore à corriger)

> Détail complet dans l'échange précédent. Synthèse des correctifs **toujours en attente** :

| # | Problème | Correction |
|---|----------|------------|
| 1 | `config-service` sur le **port 8080** (conflit avec la Gateway) | Le passer sur **8888** |
| 2 | **Absent du `docker-compose.yml`** alors que `discovery` en dépend | L'ajouter, démarré **en premier** |
| 3 | URLs en `localhost` (KO entre conteneurs) | Utiliser `http://config-service:8888` |
| 4 | `discovery` : `spring.config.import=configserver:` **obligatoire** | Passer en `optional:configserver:` |
| 5 | Dossier `cloud-conf/` **vide** en local | Vérifier le repo `github.com/FulCoding/cloud-conf` (doit contenir `discovery-service.yml` avec le port 8761 + réglages Eureka) |

---

## C. Checklist d'intégration finale — ✅ FAITE (8 juin 2026)

- [x] Ajouter `ai-document-service` au compose racine (port 8001, SQLite, volumes)
- [x] Ajouter la route gateway `/api/v1/**` (URL directe `http://ai-document-service:8001`)
- [x] Mettre à jour le contrat REST avec les chemins OCR réels
- [x] Corriger `config-service` : port **8888**, ajouté au compose (+ Dockerfile créé), URL `config-service:8888`
- [x] `discovery-service` : import `optional:` + valeurs locales de secours (port 8761, Eureka) + `CONFIG_SERVER_URI` en env
- [x] Vérifier le repo `cloud-conf` : accessible, contient `discovery-service.properties` (port 8761) ✅
- [x] `docker compose up -d --build` + tests OK : login→token→OCR via gateway (401 sans token, 200 avec)

> Détails restants mineurs : le repo distant `cloud-conf` a une coquille
> (`eureka.client.register.with.eureka` au lieu de `register-with-eureka`) — sans
> effet car le fallback local de `discovery` couvre le cas. Le `Docker-compose.yml`
> interne à `ai-document-service` est conservé pour le dev isolé uniquement.
```
