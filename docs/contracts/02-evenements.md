# Contrats d'événements (asynchrone — RabbitMQ)

Quand un service fait quelque chose d'important, il **publie un événement** sans
savoir qui l'écoute. D'autres services y **réagissent**. C'est le découplage : le
service qui publie ne dépend pas de ceux qui consomment.

> 📨 **Exemple** : `loan-service` approuve un prêt → publie `PretApprouve`.
> `notification-service` l'écoute → envoie un SMS. `account-service` l'écoute →
> verse les fonds. Le service de prêt n'a **rien à savoir** d'eux.

## Configuration RabbitMQ (à fixer ensemble)
- **Exchange** : `bank.events` (type `topic`)
- **Routing key** : `<domaine>.<evenement>` (ex : `loan.approved`)
- Chaque service consommateur déclare **sa propre queue** liée aux clés qui l'intéressent.
- **Format** : JSON. Enveloppe commune ci-dessous.

### Enveloppe commune (tous les événements)
```json
{
  "eventId": "uuid",
  "eventType": "PretApprouve",
  "occurredAt": "2026-06-06T12:00:00Z",
  "source": "loan-service",
  "data": { /* charge utile propre à l'événement */ }
}
```

---

## Catalogue des événements

| Événement (routing key) | Produit par | Consommé par | `data` (charge utile) |
|-------------------------|-------------|--------------|------------------------|
| `customer.created`<br/>**ClientCree** | customer | notification | `{ clientId, email, nom }` |
| `customer.kyc.validated`<br/>**ClientKycValide** | customer | loan, notification | `{ clientId, statutKYC }` |
| `account.created`<br/>**CompteCree** | account | notification | `{ compteId, clientId, numeroCompte }` |
| `transaction.completed`<br/>**TransactionEffectuee** | transaction | account, notification | `{ transactionId, type, compteSourceId, compteDestId, montant, devise }` |
| `transaction.failed`<br/>**TransactionEchouee** | transaction | notification | `{ transactionId, raison }` |
| `document.verified`<br/>**DocumentVerifie** | ai-document | customer, loan | `{ documentId, clientId, type, scoreConfiance, donneesStructurees }` |
| `loan.approved`<br/>**PretApprouve** | loan | account, notification | `{ pretId, clientId, compteId, montantAccorde }` |
| `loan.rejected`<br/>**PretRejete** | loan | notification | `{ demandeId, clientId, motif }` |
| `loan.installment.overdue`<br/>**EcheanceImpayee** | loan | notification | `{ pretId, echeanceNumero, montant }` |

### Exemples de charge utile

**TransactionEffectuee** (`transaction.completed`)
```json
{
  "eventType": "TransactionEffectuee",
  "data": {
    "transactionId": 100, "type": "TRANSFERT",
    "compteSourceId": 10, "compteDestId": 22,
    "montant": 50000.00, "devise": "XAF"
  }
}
```

**PretApprouve** (`loan.approved`)
```json
{
  "eventType": "PretApprouve",
  "data": { "pretId": 40, "clientId": 5, "compteId": 10, "montantAccorde": 1000000.00 }
}
```

---

## Qui écoute quoi (résumé pour le développement)

- **notification-service** écoute **presque tout** → envoie SMS/email/push.
- **account-service** écoute `loan.approved` (verser les fonds) et `transaction.completed` (mettre à jour le solde si l'archi le prévoit ainsi).
- **loan-service** écoute `customer.kyc.validated` et `document.verified` (débloquer l'analyse du dossier).
- **customer-service** écoute `document.verified` (mettre à jour le KYC).

> 💡 Pour le TP, commencez **simple** : faites d'abord fonctionner le REST (synchrone)
> de bout en bout, puis ajoutez les événements service par service. Ne bloquez pas
> le développement sur la messagerie dès le départ.
