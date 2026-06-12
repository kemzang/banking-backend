# Transaction Service

Microservice du TP INF462 responsable des depots, retraits, transferts et de
l'historique des operations bancaires. Il ecoute sur le port `8083` et stocke
ses donnees dans PostgreSQL, base `bank_transaction_db`.

## Bounded Context

Le bounded context **Transaction** decide si une operation peut etre initiee,
calcule sa commission, conserve son statut et publie les evenements metier.
La mise a jour effective des soldes appartient au bounded context **Account**
et passe par `account-service`.

L'agregat principal est `Transaction`. Une transaction est toujours persistee
avec le statut `INITIEE`, puis devient `VALIDEE` ou `REJETEE`. Les transactions
rejetees ne sont jamais supprimees.

## API

| Methode | Endpoint | Description |
|---|---|---|
| POST | `/api/transactions/deposit` | Crediter un compte |
| POST | `/api/transactions/withdraw` | Debiter un compte |
| POST | `/api/transactions/transfer` | Transferer entre deux comptes |
| GET | `/api/transactions/{id}` | Consulter une transaction |
| GET | `/api/transactions?accountId=1` | Historique complet d'un compte |

Swagger UI : `http://localhost:8083/swagger-ui.html`

Description OpenAPI : `http://localhost:8083/v3/api-docs`

## Regles metier

- les montants utilisent exclusivement `BigDecimal` et doivent etre positifs ;
- le plafond par defaut est `1 000 000` ;
- la devise demandee doit correspondre a celle des comptes ;
- la commission standard est de `0,5 %` et est supportee par la source ;
- la commission est nulle par defaut entre comptes du meme operateur ;
- le debit d'un transfert porte sur `montant + commission` ;
- tout rejet conserve un motif dans l'historique.

## Design Patterns

- **Service Layer** : orchestration dans `TransactionServiceImpl` ;
- **Repository** : persistance JPA de l'agregat ;
- **Mapper** : conversion Entity vers DTO sans exposer JPA ;
- **Adapter/Gateway** : `AccountClient` et `RestAccountClient` ;
- **Strategy** : calcul de commission configurable ;
- **Publisher/Adapter** : abstraction Kafka ou logs ;
- **Saga par compensation** : correction d'un debit distant deja effectue ;
- **Global Exception Handler** : erreurs HTTP uniformes.

## Communications

La communication synchrone utilise un `RestClient` load-balance vers
`http://account-service` :

- `GET /api/accounts/{id}` pour la devise et l'operateur ;
- `POST /api/accounts/{id}/credit` avec `{"montant": 1000}` ;
- `POST /api/accounts/{id}/debit` avec `{"montant": 1000}`.

La communication asynchrone publie :

- `transaction.completed` ;
- `transaction.rejected` ;
- `transaction.compensation.requested`.

Par defaut, `LogTransactionEventPublisher` permet de demarrer sans broker.
Pour Kafka :

```bash
TRANSACTION_EVENTS_BROKER=kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

## Saga et coherence distribuee

Une transaction JPA locale ne peut pas annuler un debit deja accepte par un
autre microservice. Le cas critique est :

1. debit de la source reussi ;
2. credit de la destination echoue ;
3. transaction marquee `REJETEE` ;
4. publication de `transaction.compensation.requested` avec le montant total
   debite.

Dans le MVP, le consommateur idempotent qui recreditera la source reste a
implementer dans `account-service`. Une evolution production utiliserait un
Transactional Outbox pour garantir atomiquement persistance et publication.

## Configuration

Variables principales :

| Variable | Valeur par defaut |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/bank_transaction_db` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `EUREKA_URL` | `http://localhost:8761/eureka` |
| `TRANSACTION_MAX_AMOUNT` | `1000000` |
| `TRANSACTION_COMMISSION_PERCENTAGE` | `0.005` |
| `TRANSACTION_EVENTS_BROKER` | `log` |

## Execution

```bash
./mvnw clean package
java -jar target/transaction-service-0.0.1-SNAPSHOT.jar
```

Sous Windows :

```powershell
.\mvnw.cmd clean package
```

Docker :

```bash
docker network create banking-network
docker compose up --build
```

Le compose est volontairement partiel. `discovery-service`, `account-service`
et Kafka peuvent rejoindre le reseau externe `banking-network`.

## Demonstration manuelle

1. Creer deux comptes dans `account-service`, en notant leurs identifiants.
2. Faire un depot de `50000 XAF` sur le compte source.
3. Faire un retrait simple de `10000 XAF`.
4. Transferer `25000 XAF` vers le compte destination.
5. Verifier les deux soldes via `account-service`.
6. Appeler `GET /api/transactions?accountId={sourceId}`.
7. Tester un retrait superieur au solde et verifier le rejet `409`.
8. Tester un transfert superieur au plafond et verifier le rejet `400`.
9. Simuler l'echec du credit destination et observer le rejet puis l'evenement
   `transaction.compensation.requested`.

Exemple de depot :

```bash
curl -X POST http://localhost:8083/api/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{"compteId":1,"montant":50000,"devise":"XAF"}'
```

Exemple de transfert :

```bash
curl -X POST http://localhost:8083/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{"compteSourceId":1,"compteDestId":2,"montant":25000,
       "devise":"XAF","motif":"Paiement fournisseur"}'
```

## Tests

```bash
./mvnw test
```

Les tests couvrent la logique metier, les rejets, le plafond, la devise, la
compensation Saga, les lectures d'historique et les cinq routes HTTP.
