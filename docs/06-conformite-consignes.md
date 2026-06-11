# Conformité aux consignes du TP — état au 11 juin 2026

> Comparaison **honnête** entre les exigences du sujet (INF462) et l'état réel du projet.
> Légende : ✅ fait · 🟡 partiel / amorcé · ❌ pas commencé

## A. Fonctionnalités métier demandées

| # | Exigence du sujet | État | Commentaire |
|---|-------------------|------|-------------|
| 1 | Inscription et gestion des clients | ✅ | customer-service (CRUD + KYC) + front |
| 2 | Gestion des comptes financiers | ❌ | account-service = squelette vide |
| 3 | Opérations de dépôt et retrait | ❌ | transaction-service vide |
| 4 | Transferts intra / inter-opérateurs | ❌ | transaction-service vide |
| 5 | Demandes de prêts | ❌ | loan-service vide |
| 6 | Analyse / validation des dossiers de prêts | ❌ | loan-service vide |
| 7 | Génération d'échéanciers | ❌ | loan-service vide |
| 8 | Remboursement des prêts | ❌ | loan-service vide |
| 9 | Notifications liées aux opérations | ❌ | notification-service = squelette |
| 10 | Gestion des administrateurs et opérateurs | 🟡 | Opérateurs ✅ ; rôles ADMIN/OPERATEUR existent mais pas d'espace d'administration dédié |
| 11 | Rapports et statistiques | ❌ | non commencé |
| 12 | Audits et traçabilité | ❌ | non commencé |
| 13 | Plusieurs mécanismes d'authentification | 🟡 | **un seul** mécanisme (JWT email/mot de passe). Le sujet en demande **plusieurs** (ex. OTP/SMS, OAuth…) |

## B. Gestion documentaire & IA (OCR)

| Exigence | État | Commentaire |
|----------|------|-------------|
| Soumission de documents (CNI, passeport…) | 🟡 | Upload + OCR OK, mais pas encore relié à un **client** ni à un **type** de document |
| Extraction OCR | ✅ | ai-document-service (Tesseract) — fonctionnel |
| Vérification automatique d'informations | ❌ | non commencé |
| Alimentation des processus (ouverture compte, prêts) | ❌ | OCR pas branché sur le KYC / les prêts |
| Automatisation partielle de la décision | ❌ | non commencé |

## C. Démarche d'ingénierie & architecture

| Exigence | État | Commentaire |
|----------|------|-------------|
| Analyse DDD (sous-domaines, bounded contexts, agrégats, événements) | 🟡 | Document structuré mais **à remplir** ([01-analyse-ddd.md](01-analyse-ddd.md)) — **critère d'éval majeur** |
| Découpage microservices justifié | 🟡 | Amorcé ([03-modele-domaine-et-plan.md](03-modele-domaine-et-plan.md)) |
| Diagrammes UML (cas d'usage, classes, MCD/MLD) | 🟡 | Présents en Mermaid, à compléter/exporter |
| **Multi-techno Java + JavaScript + Python** | 🟡 | Java ✅ (services), Python ✅ (OCR). **JavaScript** : le front Angular est en TypeScript ; le **microservice Node (notification) n'est pas encore codé** → à faire pour satisfaire strictement la consigne |
| Découverte de services | ✅ | Eureka (discovery-service) |
| Configuration centralisée | ✅ | config-service (Spring Cloud Config) |
| Routage des requêtes (API Gateway) | ✅ | gateway-service |
| Sécurité des échanges | ✅ | JWT validé à la gateway |
| Résilience (circuit breaker, retries) | ❌ | non mis en place |
| Communications **synchrones** | ✅ | REST via gateway + Eureka |
| Communications **asynchrones** | ❌ | RabbitMQ déployé mais **aucun événement** publié/consommé |
| Gestion de la concurrence des transactions | ❌ | dépend de transaction-service |
| Supervision / observabilité / journalisation | ❌ | non mis en place (pas de logs centralisés/métriques) |
| Interface moderne (espaces client/admin/opérateur) | 🟡 | Front Angular fonctionnel, mais **pas d'espaces différenciés par rôle** |

## D. Cloud Native & déploiement

| Exigence | État | Commentaire |
|----------|------|-------------|
| Conteneurisation Docker | ✅ | Dockerfiles + docker-compose |
| Déploiement Kubernetes | ❌ | aucun manifeste `k8s/` |
| Pipeline CI/CD | ❌ | aucun workflow |

## E. Livrables attendus

| # | Livrable | État |
|---|----------|------|
| 1 | Cahier des charges | 🟡 squelette |
| 2 | Étude DDD complète | 🟡 squelette |
| 3 | Architecture microservices justifiée | 🟡 amorcé |
| 4 | Diagrammes UML | 🟡 amorcé |
| 5 | Code source sur GitHub | ✅ |
| 6 | Documentation technique + API | 🟡 (contracts + Swagger Python) |
| 7 | Fichiers Docker | ✅ |
| 8 | Manifests Kubernetes | ❌ |
| 9 | Doc CI/CD | ❌ |
| 10 | Démonstration en production | ❌ |
| 11 | Rapport technique | ❌ |

---

## Verdict

**Ce qui est implémenté respecte bien les consignes** sur le plan **architectural** : l'approche
microservices, le polyglottisme (Java/Python, JS à finaliser), l'API Gateway, la découverte
de services (Eureka), la configuration centralisée, la sécurité JWT, l'OCR comme service
distribué et la conteneurisation Docker sont **conformes et de bonne qualité**.

**Mais la couverture est partielle** : l'essentiel des **fonctionnalités métier**
(comptes, transactions, prêts, notifications, rapports, audit) et plusieurs **exigences
transverses** (communications asynchrones, K8s, CI/CD, observabilité, DDD complet,
rapport) restent à faire.

## Priorités recommandées (pour maximiser la note)

1. **Remplir l'analyse DDD + le cahier des charges** (critères d'éval les plus pondérés).
2. **account-service → transaction-service → loan-service** (le cœur métier bancaire).
3. **notification-service (Node) + 1ᵉʳ événement RabbitMQ** → coche « JavaScript » **et** « async ».
4. **Relier l'OCR à un client** (type de document + statut) → coche « alimentation des processus ».
5. **Manifests Kubernetes + un workflow CI/CD GitHub Actions**.
6. **Espaces différenciés par rôle** dans le front (client / opérateur / admin).
7. **Rapport technique** + préparer la **démo**.
