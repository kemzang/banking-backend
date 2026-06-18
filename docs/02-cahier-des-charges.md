# Cahier des charges — Plateforme Bancaire Distribuée

> Livrable n°1 du TP INF462.

---

## 1. Contexte et objectif

Concevoir et développer une **plateforme financière distribuée** permettant à
plusieurs opérateurs financiers (banques, microfinances, opérateurs mobiles) de
collaborer dans un même écosystème, avec une expérience utilisateur homogène,
sécurisée et transparente, malgré des règles métier propres à chacun.

## 2. Acteurs du système

| Acteur | Rôle |
|--------|------|
| Client | S'inscrit, gère ses comptes, effectue des opérations, demande des prêts |
| Opérateur financier | Définit ses règles (plafonds, commissions, validations) |
| Administrateur | Gère opérateurs, utilisateurs, supervise la plateforme |
| Système IA/OCR | Extrait et vérifie automatiquement les informations des documents |

---

## 3. Exigences fonctionnelles

> Format conseillé : `EF-xx` — En tant que `<acteur>`, je veux `<action>` afin de `<but>`.

| ID | Exigence |
|----|----------|
| EF-01 | Inscription et gestion des clients |
| EF-02 | Gestion des comptes financiers (ouverture, consultation, clôture) |
| EF-03 | Opérations de dépôt et de retrait |
| EF-04 | Transferts intra-opérateurs |
| EF-05 | Transferts inter-opérateurs |
| EF-06 | Demande de prêt |
| EF-07 | Analyse et validation des dossiers de prêt |
| EF-08 | Génération d'échéanciers de remboursement |
| EF-09 | Remboursement des prêts |
| EF-10 | Notifications liées aux opérations (multi-canal) |
| EF-11 | Gestion des administrateurs et des opérateurs |
| EF-12 | Génération de rapports et statistiques |
| EF-13 | Gestion des audits et de la traçabilité |
| EF-14 | Plusieurs mécanismes d'authentification |
| EF-15 | Soumission de documents (CNI, passeport, justificatif de domicile, bulletins de salaire, relevés, contrats, documents administratifs) |
| EF-16 | Extraction automatique des informations par OCR |
| EF-17 | Vérification automatique de certaines informations |
| EF-18 | Alimentation des processus métier (ouverture de compte, prêts) par l'IA |
| EF-19 | Automatisation partielle de la prise de décision |

---

## 4. Exigences non fonctionnelles

| ID | Catégorie | Exigence |
|----|-----------|----------|
| ENF-01 | Disponibilité | Haute disponibilité, tolérance aux pannes |
| ENF-02 | Scalabilité | Montée en charge, scalabilité horizontale par service |
| ENF-03 | Sécurité | Authentification forte, chiffrement, secrets externalisés |
| ENF-04 | Traçabilité | Journalisation et audit de toutes les opérations sensibles |
| ENF-05 | Performance | Temps de réponse acceptable sous forte charge |
| ENF-06 | Résilience | Circuit breaker, retries, dégradation gracieuse |
| ENF-07 | Observabilité | Logs centralisés, métriques, supervision |
| ENF-08 | Maintenabilité | Architecture modulaire, code documenté, tests |
| ENF-09 | Évolutivité | Ajout de nouveaux opérateurs/services sans interruption majeure |
| ENF-10 | Cohérence | Gestion de la concurrence des transactions |

---

## 5. Contraintes techniques (imposées par le sujet)

- Architecture **microservices** distribuée, communications synchrones **et** asynchrones.
- **Polyglotte obligatoire** : Java + JavaScript + Python (répartition justifiée).
- Mécanismes : découverte de services, configuration centralisée, routage (API Gateway),
  résilience, sécurité, concurrence, supervision.
- **OCR + IA** intégrés comme services distribués (technos libres).
- Conteneurisation **Docker** + déploiement **Kubernetes**.
- Pipeline **CI/CD**.
- Interface utilisateur moderne (techno libre, justifiée) avec espaces client / admin / opérateur.

## 6. Périmètre / Hors-périmètre

- **Inclus** : inscription/authentification (mot de passe + Google), gestion des clients & KYC,
  opérateurs, comptes (ouverture/solde/statuts), transactions (dépôt/retrait/transfert),
  prêts (demande/décision/échéancier/remboursement), OCR de documents, notifications
  asynchrones, espaces par rôle, supervision (Prometheus/Grafana), conteneurisation & K8s & CI/CD.
- **Hors-périmètre** (pour ce TP) : intégration à un système bancaire réel / interbancaire,
  paiements externes réels, envoi SMS/email réel en production (simulé via Ethereal),
  vérification biométrique, conformité réglementaire complète (AML/lutte anti-fraude avancée).

## 8. Exemples de critères d'acceptation
- **EF-03 Dépôt** : un dépôt d'un montant > 0 sur un compte ACTIF augmente le solde du même montant et crée une transaction `VALIDEE` (HTTP 201).
- **EF-04/05 Transfert** : un transfert débite la source (refus `409` si solde insuffisant) et crédite la destination ; une commission est calculée.
- **EF-07/08 Prêt** : l'approbation d'une demande crée un prêt et génère un échéancier à mensualités constantes.
- **EF-10 Notification** : toute transaction validée produit une notification consultable et un email.
- **ENF-03 Sécurité** : tout endpoint (hors /api/auth/**) refuse l'accès sans JWT valide (`401`).

## 7. Critères d'acceptation et d'évaluation

Voir la grille du sujet : qualité de l'analyse métier, pertinence du DDD et du découpage,
qualité de l'architecture, design/architectural patterns, qualité du code, intégration
OCR/IA, UI/UX, sécurité/dispo/résilience, communications sync/async, technologies Cloud
Native, déploiement/observabilité, DevOps/CI-CD, documentation, démonstration.
