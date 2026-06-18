# Audit final vs énoncé — 16 juin 2026

Vérification du code réel contre chaque exigence du sujet INF462.
✅ fait & testé · 🟡 partiel · ❌ manquant

## A. Services back-end (état réel)

| Service | Techno | Implémenté | Testé |
|---------|--------|-----------|-------|
| discovery (Eureka) | Java | ✅ | ✅ |
| config-service | Java | ✅ | ✅ |
| gateway (routage + JWT) | Java | ✅ | ✅ |
| auth (login/JWT + Google) | Java | ✅ | ✅ (Google: code prêt, besoin Client ID) |
| customer (clients + opérateurs) | Java | ✅ | ✅ |
| account (comptes) | Java | ✅ | ✅ |
| transaction (dépôt/retrait/transfert) | Java | ✅ | ✅ |
| loan (prêts + échéancier) | Java | ✅ | ✅ |
| ai-document (OCR) | Python | ✅ | ✅ |
| notification (async + email) | Node | ✅ | ✅ |

➡️ **Tous les services tournent** (12 conteneurs) et le parcours métier marche de bout en bout.

## B. Interfaces front (Angular)

| Page | État |
|------|------|
| Login / Register (+ bouton Google) | ✅ |
| Dashboard (compteurs) | ✅ |
| Clients (CRUD + KYC) | ✅ |
| Comptes | ✅ |
| Transactions | ✅ |
| Prêts | ✅ |
| Notifications | ✅ |
| Opérateurs | ✅ |
| **Espaces différenciés client / admin / opérateur** | ❌ (toutes les pages visibles par tous) |
| **Page Documents reliée à un client** | 🟡 (OCR isolé, pas rattaché au dossier client) |
| **Rapports & statistiques** | ❌ |

## C. Exigences fonctionnelles de l'énoncé

| Fonction demandée | État |
|-------------------|------|
| Inscription & gestion des clients | ✅ |
| Gestion des comptes | ✅ |
| Dépôt / retrait | ✅ |
| Transferts intra-opérateurs | ✅ |
| Transferts **inter-opérateurs** | 🟡 (champs présents, règles/commissions inter-op. non spécifiques) |
| Demande de prêt | ✅ |
| Analyse / validation des dossiers | ✅ (décision manuelle) |
| Génération d'échéanciers | ✅ |
| Remboursement | ✅ |
| Notifications liées aux opérations | ✅ (async RabbitMQ + email) |
| Gestion administrateurs & opérateurs | 🟡 (opérateurs ✅ ; pas d'espace admin ni de gestion des admins) |
| **Génération de rapports & statistiques** | ❌ |
| **Gestion des audits & traçabilité** | ❌ |
| Plusieurs mécanismes d'authentification | ✅ (mot de passe + Google) |

## D. Gestion documentaire & IA (énoncé)

| Exigence | État |
|----------|------|
| Soumettre des documents (CNI, passeport…) | 🟡 (upload OCR ok, mais type/document non typé ni lié au client) |
| Extraction OCR | ✅ |
| **Vérification automatique d'informations** | ❌ |
| **Alimentation des processus (ouverture compte, prêt)** | ❌ (OCR pas branché sur KYC/prêt) |
| **Automatisation partielle de la décision** | ❌ |

## E. Exigences techniques / transverses

| Exigence | État |
|----------|------|
| Architecture microservices | ✅ |
| Multi-techno Java + JS + Python | ✅ |
| Découverte de services (Eureka) | ✅ |
| Configuration centralisée | ✅ |
| API Gateway / routage | ✅ |
| Sécurité des échanges (JWT) | ✅ |
| Communications synchrones | ✅ |
| Communications asynchrones | ✅ (RabbitMQ) |
| **Résilience (circuit breaker, retries)** | ❌ |
| **Gestion concurrence des transactions** | 🟡 (transactions DB locales ; pas de saga/verrou distribué) |
| **Supervision / observabilité / journalisation centralisée** | ❌ (seulement /actuator/health) |
| Conteneurisation Docker (back) | ✅ |
| **Conteneurisation du frontend** | ❌ (pas de Dockerfile front) |
| **Déploiement Kubernetes** | ❌ |
| **Pipeline CI/CD** | ❌ |

## F. Livrables attendus (11)

| # | Livrable | État |
|---|----------|------|
| 1 | Cahier des charges | 🟡 squelette |
| 2 | Étude DDD complète | 🟡 squelette (à remplir — gros critère) |
| 3 | Architecture microservices justifiée | 🟡 amorcé |
| 4 | Diagrammes UML | 🟡 (use-case, classes, MCD/MLD en Mermaid ; manque séquence/déploiement) |
| 5 | Code source GitHub | ✅ |
| 6 | Doc technique + API | 🟡 (contracts + Swagger transaction/OCR ; pas centralisé) |
| 7 | Fichiers Docker | ✅ (back) / ❌ (front) |
| 8 | Manifests Kubernetes | ❌ |
| 9 | Doc CI/CD | ❌ |
| 10 | Démo en production | ❌ |
| 11 | Rapport technique | ❌ |

---

## G. CE QUI RESTE À FAIRE (priorisé)

### Bloc 1 — Cloud Native / DevOps (fort poids « technologies Cloud Native » + « DevOps/CI-CD »)
1. **Dockerfile du frontend** (+ l'ajouter au compose) → « entièrement conteneurisé ».
2. **Manifests Kubernetes** (`k8s/`) pour tous les services + ingress.
3. **Pipeline CI/CD** (`.github/workflows/`) : build + tests + images Docker.
4. **Observabilité** : actuator/prometheus + (option) Grafana, logs.

### Bloc 2 — Fonctionnel manquant
5. **Espaces par rôle** dans le front (client / opérateur / admin) + garde par rôle.
6. **OCR relié au métier** : rattacher un document à un `clientId` + type, et alimenter le KYC.
7. **Service de rapports & statistiques** (ou endpoints d'agrégation).
8. **Audit & traçabilité** (journal des opérations).
9. **Résilience** : resilience4j (circuit breaker) sur les appels inter-services.

### Bloc 3 — Documentation & livrables (gros poids dans la note)
10. **Remplir l'étude DDD** (sous-domaines, bounded contexts, agrégats, événements).
11. **Cahier des charges** complet.
12. **Rapport technique** (choix, difficultés, compromis, perspectives).
13. **Diagrammes de séquence + déploiement**.
14. Préparer la **démo**.

> Le **cœur fonctionnel est fait et fonctionne**. Ce qui reste est surtout :
> **(a)** le volet Cloud Native/DevOps (K8s, CI/CD, conteneurisation front),
> **(b)** quelques fonctions secondaires (rapports, audit, OCR↔métier, rôles UI),
> **(c)** la **documentation** (DDD, cahier des charges, rapport) — fortement notée.
