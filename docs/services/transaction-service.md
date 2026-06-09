# transaction-service — Dépôts, retraits, transferts

**Port** 8083 · **Base** `bank_transaction_db` · **Package** `com.banking.transaction_service`
Rôle : enregistrer les opérations et **mettre à jour les soldes** en appelant
`account-service`.

> ⚠️ Ce service **appelle account-service** → lire la section « Communication entre
> services » du [README](README.md#communication-entre-services-important-pour-transaction--loan).

---

## 1. Entité `Transaction` (package `entity`)

| Attribut | Type | Notes |
|----------|------|-------|
| id | `Long` | `@Id @GeneratedValue(IDENTITY)` |
| reference | `String` | `@Column(unique=true)` — générée (ex. `TX-2026-000123`) |
| type | `TypeTransaction` | `@Enumerated(STRING)` |
| montant | `BigDecimal` | > 0 |
| devise | `String` | |
| compteSourceId | `Long` | null si DEPOT |
| compteDestId | `Long` | null si RETRAIT |
| operateurSourceId | `Long` | (inter-opérateurs, optionnel) |
| operateurDestId | `Long` | |
| commission | `BigDecimal` | calculée |
| statut | `StatutTransaction` | `@Enumerated(STRING)` |
| motif | `String` | |
| dateOperation | `LocalDateTime` | `@PrePersist` |

**Enums :**
```java
public enum TypeTransaction { DEPOT, RETRAIT, TRANSFERT }
public enum StatutTransaction { INITIEE, VALIDEE, REJETEE }
```

## 2. Repository
```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCompteSourceIdOrCompteDestId(Long src, Long dest);
}
```

## 3. DTO (records)
```java
public record DepotRequestDTO(Long compteId, BigDecimal montant, String devise) {}
public record RetraitRequestDTO(Long compteId, BigDecimal montant, String devise) {}
public record TransfertRequestDTO(Long compteSourceId, Long compteDestId,
                                  BigDecimal montant, String devise, String motif) {}
public record TransactionResponseDTO(Long id, String reference, TypeTransaction type,
                                     BigDecimal montant, String devise, BigDecimal commission,
                                     StatutTransaction statut, LocalDateTime dateOperation) {}
```

## 4. Service — logique (cœur du service)

Pour chaque opération : créer la transaction `INITIEE`, appeler account-service,
puis passer `VALIDEE` (ou `REJETEE` si l'appel échoue).

- **depot(dto)** : `account-service` `POST /api/accounts/{compteId}/credit {montant}` → si OK, transaction `DEPOT` `VALIDEE`.
- **retrait(dto)** : `POST /api/accounts/{compteId}/debit {montant}` → si `409` (solde insuffisant), transaction `REJETEE` (et renvoyer 409) ; sinon `VALIDEE`.
- **transfert(dto)** :
  1. `commission = calcul(montant)` (ex. 0,5 % ; 0 si même opérateur — à définir) ;
  2. **débiter** la source de `montant + commission` ;
  3. **créditer** la destination de `montant` ;
  4. transaction `TRANSFERT` `VALIDEE`.
  > Si le débit échoue → on n'effectue pas le crédit, transaction `REJETEE`.
- **getById / getByCompte** : lecture.

> 💡 La cohérence multi-comptes (si le crédit échoue après le débit) relève des
> **transactions distribuées** (saga/compensation) — pour le TP, gérer le cas simple
> et documenter la limite dans le rapport.

## 5. Controller `@RequestMapping("/api/transactions")`

| Méthode | Chemin | Entrée | Retour | Code |
|---------|--------|--------|--------|------|
| POST | `/api/transactions/deposit` | `DepotRequestDTO` | `TransactionResponseDTO` | **201** |
| POST | `/api/transactions/withdraw` | `RetraitRequestDTO` | `TransactionResponseDTO` | **201** (409 si solde insuffisant) |
| POST | `/api/transactions/transfer` | `TransfertRequestDTO` | `TransactionResponseDTO` | **201** |
| GET | `/api/transactions/{id}` | — | `TransactionResponseDTO` | 200 (404) |
| GET | `/api/transactions?accountId={id}` | — | `List<TransactionResponseDTO>` | 200 |

**Exemple de retour :**
```json
// POST /api/transactions/transfer -> 201
{ "id": 100, "reference": "TX-2026-000100", "type": "TRANSFERT",
  "montant": 50000.00, "devise": "XAF", "commission": 250.00,
  "statut": "VALIDEE", "dateOperation": "2026-06-08T12:00:00" }
```

## 6. Intégration
- **Gateway** : `lb://transaction-service` sur `/api/transactions/**` (déjà là).
- **Appelle** : `account-service` (credit/debit) via `RestClient` `@LoadBalanced`.
- **Événement à publier** (plus tard) : `TransactionEffectuee` (`transaction.completed`) → consommé par notification.
- Ajouter la dépendance `spring-cloud-starter-loadbalancer` si besoin.

## 7. Checklist
- [ ] entité + 2 enums + repository
- [ ] DTO (4 records)
- [ ] `RestClient` `@LoadBalanced` (config) + appels account-service
- [ ] service (depot/retrait/transfert + commission + statuts)
- [ ] controller (5 endpoints)
- [ ] config (port 8083, base `bank_transaction_db`)
- [ ] test : dépôt puis transfert entre 2 comptes, vérifier les soldes via account-service
