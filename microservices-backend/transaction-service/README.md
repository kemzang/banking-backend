# transaction-service - Saga et evenements

## Pourquoi une Saga

Un transfert modifie deux comptes au travers de plusieurs appels HTTP. Une
transaction JPA locale ne controle que la base `bank_transaction_db` et ne peut
pas annuler automatiquement une modification deja validee par `account-service`.

Le cas critique est le suivant :

1. le compte source est debite de `montant + commission` ;
2. le credit du compte destination echoue ;
3. la transaction locale est marquee `REJETEE`, mais le debit distant existe
   toujours.

Le service publie alors l'evenement
`transaction.compensation.requested`. Il contient la transaction, le compte
source et le montant total debite. Un consommateur dans `account-service` pourra
recrediter le compte source de maniere idempotente.

## Evenements publies

- `transaction.completed` : operation validee ;
- `transaction.rejected` : operation rejetee avec son motif ;
- `transaction.compensation.requested` : debit source a compenser.

La reference de transaction est utilisee comme cle Kafka afin de conserver
l'ordre des messages d'une meme transaction.

## Configuration

Par defaut, `transaction.events.broker=log` utilise
`LogTransactionEventPublisher`. Le service fonctionne donc sans broker.

Pour activer Kafka :

```bash
TRANSACTION_EVENTS_BROKER=kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

Les topics sont configurables dans `application.yml`.

## Limite du MVP

La publication prepare la Saga, mais le consommateur de compensation et son
traitement idempotent restent a implementer dans `account-service`. Pour une
garantie stricte entre sauvegarde JPA et publication Kafka, une evolution vers
le pattern Transactional Outbox sera necessaire.
