# Contrats d'interface (API & événements)

## À quoi sert ce dossier ?

Ces fichiers définissent **comment les services se parlent**. C'est un **accord
d'équipe** : tant que personne ne change ces contrats sans prévenir, chacun peut
développer son service de son côté **sans casser celui des autres**.

> 🔌 **Analogie** : on fixe la forme de la prise avant de fabriquer l'appareil.
> Dev 3 peut coder « appeler le client » alors que `customer-service` n'est pas
> fini, parce que la forme de la réponse est déjà connue.

- [`01-api-rest.md`](01-api-rest.md) — les appels **synchrones** (REST) : URL, méthode, JSON.
- [`02-evenements.md`](02-evenements.md) — les messages **asynchrones** (RabbitMQ).

**Règle d'or** : on modifie un contrat → on prévient l'équipe (message + petite PR).

---

## Conventions communes (valables pour TOUS les services)

### 1. Point d'entrée unique : la Gateway
Le frontend et les appels externes passent **toujours** par la Gateway :
`http://localhost:8080`. Elle route vers le bon service :

| Préfixe d'URL | Service cible |
|---------------|---------------|
| `/api/auth/**` | auth-service |
| `/api/customers/**` | customer-service |
| `/api/operators/**` | customer-service |
| `/api/accounts/**` | account-service |
| `/api/transactions/**` | transaction-service |
| `/api/loans/**` | loan-service |
| `/api/documents/**` | ai-document-service |

> En interne, un service appelle un autre via Eureka : `http://customer-service/api/customers/5`
> (le nom du service, pas `localhost`). On n'écrit **jamais** d'IP en dur.

### 2. Authentification
Toute requête (sauf `/api/auth/register` et `/api/auth/login`) doit porter le jeton :
```
Authorization: Bearer <token-JWT>
```

### 3. Format des dates
ISO-8601 : `2026-06-22T10:30:00Z`.

### 4. Format des montants
Nombre décimal + devise séparée : `"montant": 50000.00, "devise": "XAF"`.

### 5. Format d'erreur (identique partout)
```json
{
  "timestamp": "2026-06-06T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Client introuvable (id=5)",
  "path": "/api/customers/5"
}
```

### 6. Codes HTTP utilisés
| Code | Sens |
|------|------|
| 200 | OK (lecture/maj réussie) |
| 201 | Créé (POST réussi) |
| 400 | Requête invalide (validation) |
| 401 | Non authentifié |
| 403 | Non autorisé (mauvais rôle) |
| 404 | Ressource introuvable |
| 409 | Conflit (ex : email déjà utilisé, solde insuffisant) |
| 500 | Erreur serveur |

### 7. Pagination (listes)
`GET /api/customers?page=0&size=20` → réponse :
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 134 }
```
