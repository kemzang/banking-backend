# Analyse Domain-Driven Design (DDD) — Plateforme Bancaire Distribuée

> Livrable n°2 du TP INF462. À compléter en équipe AVANT le développement.
> Présentation finale : **22 juin 2026**.

---

## 1. Langage ubiquitaire (Ubiquitous Language)

Glossaire des termes métier partagés par toute l'équipe. Un même mot = un même sens.

| Terme | Définition métier |
|-------|-------------------|
| Client | _(personne physique/morale détenant un ou plusieurs comptes)_ |
| Opérateur financier | _(banque, microfinance, opérateur mobile — possède ses propres règles)_ |
| Compte | _(à compléter)_ |
| Transaction | _(dépôt, retrait, transfert)_ |
| Transfert intra-opérateur | _(à compléter)_ |
| Transfert inter-opérateur | _(à compléter)_ |
| Prêt | _(à compléter)_ |
| Échéancier | _(à compléter)_ |
| Plafond | _(à compléter)_ |
| Commission | _(à compléter)_ |
| ... | ... |

---

## 2. Identification des sous-domaines

Classer chaque sous-domaine et justifier (cela oriente l'effort d'ingénierie).

| Sous-domaine | Type | Justification |
|--------------|------|---------------|
| Gestion des prêts | **Core** | Cœur de la valeur métier, règles complexes |
| Transactions / Transferts | **Core** | _(à justifier)_ |
| Gestion des comptes | Core / Supporting ? | _(à justifier)_ |
| Gestion des clients (KYC) | Supporting | _(à justifier)_ |
| Documents & IA/OCR | Supporting | Alimente l'ouverture de compte et les prêts |
| Notifications | Generic | _(à justifier)_ |
| Authentification / Sécurité | Generic | _(à justifier)_ |
| Reporting & Statistiques | Supporting | _(à justifier)_ |
| Audit & Traçabilité | Supporting / Generic | _(à justifier)_ |

> **Core** = avantage concurrentiel (développer soi-même, soigner) ·
> **Supporting** = nécessaire mais pas différenciant ·
> **Generic** = solution standard/existante réutilisable.

---

## 3. Bounded Contexts (limites de contexte)

Un Bounded Context = une frontière où le modèle est cohérent. Souvent → 1 microservice.

| Bounded Context | Responsabilité | Microservice prévu | Techno |
|-----------------|----------------|--------------------|--------|
| Customer | Inscription, profil, KYC | `customer-service` | Java/Spring |
| Account | Soldes, ouverture/clôture | `account-service` | Java/Spring |
| Transaction | Dépôt, retrait, transferts | `transaction-service` | Java/Spring |
| Loan | Demande, analyse, échéancier, remboursement | `loan-service` | Java/Spring |
| Notification | Envoi multi-canal (SMS, mail, push) | `notification-service` | Node.js |
| Document/AI | OCR, extraction, vérification | `ai-document-service` | Python/FastAPI |
| Identity | Auth, rôles (client/admin/opérateur) | `auth-service` _(à créer)_ | _(à choisir)_ |

### Context Map (relations entre contextes)
Décrire les relations : _Customer/Supplier_, _Conformist_, _Anti-Corruption Layer_,
_Shared Kernel_, _Open Host Service_, etc.

```
[Customer] --(uploads docs)--> [Document/AI] --(infos extraites)--> [Loan]
[Transaction] --(événement)--> [Notification]
[Loan] --(événement)--> [Notification]
... (à compléter avec un schéma)
```

---

## 4. Agrégats, Entités, Value Objects

Pour chaque Bounded Context, définir l'**agrégat** (cluster cohérent avec une racine
garante des invariants).

### Bounded Context : Account (exemple à dupliquer)
- **Aggregate Root** : `Compte`
- **Entités** : `Compte`
- **Value Objects** : `Solde` (montant + devise), `NuméroCompte`, `Plafond`
- **Invariants métier** :
  - Le solde ne peut pas passer sous le découvert autorisé.
  - Un retrait ne peut excéder le plafond journalier.
- **Repository** : `CompteRepository`

### Bounded Context : Loan
- **Aggregate Root** : `DemandePret`
- _(à compléter)_

> _(Dupliquer ce bloc pour Customer, Transaction, etc.)_

---

## 5. Événements métier (Domain Events)

Base de la communication **asynchrone** entre services (Kafka / RabbitMQ).

| Événement | Émis par | Consommé par | Effet |
|-----------|----------|--------------|-------|
| `CompteCree` | Account | Notification | Mail de bienvenue |
| `DepotEffectue` | Transaction | Account, Notification | MAJ solde + notif |
| `TransfertInitie` | Transaction | Account (débit/crédit) | Mouvement de fonds |
| `DocumentVerifie` | Document/AI | Loan, Customer | Débloque l'étape suivante |
| `PretApprouve` | Loan | Account, Notification | Versement + échéancier |
| `EcheanceImpayee` | Loan | Notification | Relance |
| ... | ... | ... | ... |

---

## 6. Communications : synchrone vs asynchrone

| Interaction | Type | Mécanisme | Justification |
|-------------|------|-----------|---------------|
| Frontend → services | Synchrone | REST via API Gateway | Réponse immédiate attendue |
| Transaction → Notification | Asynchrone | Événement (broker) | Découplage, résilience |
| Loan → Document/AI | _(à décider)_ | _(REST ou événement)_ | _(à justifier)_ |
| ... | ... | ... | ... |

---

## 7. Justification du découpage en microservices

Synthèse : pourquoi ce découpage ? (cohésion forte intra-service, couplage faible
inter-services, alignement sur les Bounded Contexts, scalabilité indépendante,
équipes autonomes, "database per service"...).

_(à rédiger — c'est le livrable n°3)_
