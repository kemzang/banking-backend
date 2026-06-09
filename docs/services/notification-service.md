# notification-service — Notifications (Node.js)

**Port** 3000 · **Techno** Node.js (Express) · Rôle : envoyer des notifications
(SMS/email/push, **simulées** pour le TP) en réagissant aux **événements** des autres
services, et exposer l'historique.

> Service **piloté par événements** (RabbitMQ) avant tout. Pour le TP, on peut
> simuler l'envoi (un `console.log` / un enregistrement en base) sans vrai SMS.

---

## 1. Structure de projet conseillée
```
notification-service/
├── package.json
├── Dockerfile                 (déjà créé)
├── src/
│   ├── index.js               (démarre Express + le consommateur RabbitMQ)
│   ├── config.js              (variables d'env : PORT, RABBITMQ_URL, DB)
│   ├── models/notification.js (schéma/entité Notification)
│   ├── routes/notifications.js(endpoints REST)
│   ├── services/notificationService.js (création + "envoi")
│   └── events/consumer.js     (écoute les événements et crée les notifications)
└── .env.example
```
Dépendances (`package.json`) : `express`, `amqplib` (RabbitMQ), et un client BDD au
choix (`pg` pour PostgreSQL, ou `better-sqlite3`/en mémoire pour simplifier).

## 2. Donnée `Notification`
| Champ | Type | Notes |
|-------|------|-------|
| id | UUID/auto | |
| destinataireId | string | id utilisateur/client |
| canal | enum | `SMS` \| `EMAIL` \| `PUSH` |
| sujet | string | |
| contenu | string | |
| statut | enum | `EN_ATTENTE` \| `ENVOYEE` \| `ECHEC` |
| evenementSource | string | ex. `PretApprouve` |
| dateCreation | datetime | |
| dateEnvoi | datetime | nullable |

## 3. Endpoints REST (`/api/notifications`)

| Méthode | Chemin | Entrée | Retour | Code |
|---------|--------|--------|--------|------|
| GET | `/api/notifications?destinataireId={id}` | — | `Notification[]` | 200 |
| GET | `/api/notifications/{id}` | — | `Notification` | 200 (404) |
| POST | `/api/notifications` | `{ destinataireId, canal, sujet, contenu }` | `Notification` créée | 201 |

**Format de réponse** (cohérent avec le reste : JSON simple) :
```json
// GET /api/notifications?destinataireId=5 -> 200
[
  { "id": "uuid", "destinataireId": "5", "canal": "EMAIL",
    "sujet": "Compte créé", "contenu": "Votre compte CM-1-A3F9 est actif",
    "statut": "ENVOYEE", "evenementSource": "CompteCree",
    "dateCreation": "2026-06-08T12:00:00Z", "dateEnvoi": "2026-06-08T12:00:01Z" }
]
```

## 4. Consommation des événements (le cœur du service)
S'abonner à l'exchange `bank.events` (topic) et créer une notification par événement.
Voir le catalogue : [contracts/02-evenements.md](../contracts/02-evenements.md).

| Événement écouté | Notification créée |
|------------------|--------------------|
| `customer.created` (ClientCree) | « Bienvenue » → email |
| `account.created` (CompteCree) | « Compte créé » |
| `transaction.completed` (TransactionEffectuee) | « Opération effectuée » |
| `loan.approved` (PretApprouve) | « Prêt approuvé » |
| `loan.installment.overdue` (EcheanceImpayee) | « Échéance impayée » (relance) |

Logique du consommateur :
```
connexion RabbitMQ -> déclarer la queue liée à bank.events
à chaque message :
   construire une Notification (statut EN_ATTENTE)
   "envoyer" (simulation: log / appel SMS-email mock)  -> statut ENVOYEE (ou ECHEC)
   enregistrer en base
```

## 5. Intégration
- **docker-compose** (racine) : décommenter/ajouter le bloc `notification-service`
  (port 3000, `RABBITMQ_URL=amqp://guest:guest@rabbitmq:5672`, `depends_on: rabbitmq`).
- **Gateway** : ajouter une route `/api/notifications/**` → `http://notification-service:3000`
  (URL directe, comme ai-document, car service non-Java hors Eureka).
- Les autres services devront **publier** les événements (à coordonner) ; en attendant,
  on peut tester en publiant un message à la main dans la console RabbitMQ (:15672).

## 6. Checklist
- [ ] `package.json` + structure src/
- [ ] modèle Notification + accès BDD
- [ ] endpoints REST (`GET` liste, `GET` détail, `POST`)
- [ ] consommateur RabbitMQ (amqplib) abonné à `bank.events`
- [ ] simulation d'envoi (log/mock) + mise à jour du statut
- [ ] Dockerfile (présent) + entrée docker-compose + route gateway
- [ ] test : publier un événement de test → vérifier la notification créée
