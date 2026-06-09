# account-service — Gestion des comptes

**Port** 8082 · **Base** `bank_account_db` · **Package** `com.banking.account_service`
Rôle : ouvrir/consulter des comptes, gérer le solde (crédit/débit).

---

## 1. Entité `Compte` (package `entity`)

| Attribut | Type Java | Annotations / Notes |
|----------|-----------|---------------------|
| id | `Long` | `@Id @GeneratedValue(IDENTITY)` |
| numeroCompte | `String` | `@Column(unique=true)` — **généré** par le service |
| clientId | `Long` | réf. customer-service |
| operateurId | `Long` | réf. opérateur |
| type | `TypeCompte` | `@Enumerated(STRING)` |
| solde | `BigDecimal` | initialisé à `0` |
| devise | `String` | ex. `XAF` |
| plafondJournalier | `BigDecimal` | défaut ex. `1000000` |
| decouvertAutorise | `BigDecimal` | défaut `0` |
| statut | `StatutCompte` | `@Enumerated(STRING)` |
| dateOuverture | `LocalDateTime` | `@PrePersist` |

`@PrePersist` : `dateOuverture = now()`, `statut = ACTIF` si null, `solde = 0` si null,
`numeroCompte` généré si null (ex. `"CM-" + operateurId + "-" + UUID court`).

**Enums :**
```java
public enum TypeCompte { COURANT, EPARGNE }
public enum StatutCompte { ACTIF, BLOQUE, CLOTURE }
```

## 2. Repository
```java
public interface CompteRepository extends JpaRepository<Compte, Long> {
    List<Compte> findByClientId(Long clientId);
    boolean existsByNumeroCompte(String numeroCompte);
}
```

## 3. DTO (records, package `dto`)
```java
// Entrée (ouverture)
public record CompteRequestDTO(Long clientId, Long operateurId, TypeCompte type, String devise) {}

// Sortie
public record CompteResponseDTO(Long id, String numeroCompte, Long clientId,
                                TypeCompte type, BigDecimal solde, String devise,
                                StatutCompte statut) {}

// Opération crédit/débit (entrée interne)
public record MontantDTO(BigDecimal montant) {}

// Solde (sortie)
public record SoldeDTO(Long compteId, BigDecimal solde, String devise) {}
```

## 4. Service — règles métier
- **ouvrir(dto)** : crée un `Compte` (solde 0, ACTIF, numéro généré) → `save` → `CompteResponseDTO`.
- **getById(id)** : `findById().orElseThrow(404)`.
- **getByClient(clientId)** : `findByClientId`.
- **crediter(id, montant)** : `solde += montant` (montant > 0 sinon 400). `save`.
- **debiter(id, montant)** :
  - `400` si montant ≤ 0 ;
  - `409` si `solde - montant < -decouvertAutorise` (**solde insuffisant**) ;
  - `409` si `montant > plafondJournalier` ;
  - sinon `solde -= montant`, `save`.
- (optionnel) **bloquer/clôturer** : change `statut`.

## 5. Controller `@RequestMapping("/api/accounts")` — endpoints & retours

| Méthode | Chemin | Corps entrée | Retour (succès) | Code |
|---------|--------|--------------|-----------------|------|
| POST | `/api/accounts` | `CompteRequestDTO` | `CompteResponseDTO` | **201** |
| GET | `/api/accounts/{id}` | — | `CompteResponseDTO` | 200 (404 sinon) |
| GET | `/api/accounts?clientId={id}` | — | `List<CompteResponseDTO>` | 200 |
| GET | `/api/accounts/{id}/balance` | — | `SoldeDTO` | 200 |
| POST | `/api/accounts/{id}/credit` | `MontantDTO` | `CompteResponseDTO` | 200 |
| POST | `/api/accounts/{id}/debit` | `MontantDTO` | `CompteResponseDTO` | 200 (409 si solde insuffisant) |

**Exemples de retour :**
```json
// POST /api/accounts  -> 201
{ "id": 10, "numeroCompte": "CM-1-A3F9", "clientId": 5, "type": "COURANT",
  "solde": 0.00, "devise": "XAF", "statut": "ACTIF" }

// GET /api/accounts/10/balance -> 200
{ "compteId": 10, "solde": 50000.00, "devise": "XAF" }

// POST /api/accounts/10/debit  body {"montant": 999999999} -> 409
{ "timestamp": "...", "status": 409, "error": "Conflict",
  "message": "Solde insuffisant", "path": "/api/accounts/10/debit" }
```

## 6. Intégration
- **Gateway** : route `lb://account-service` sur `/api/accounts/**` (déjà présente).
- **Appelé par** `transaction-service` (credit/debit) — voir son guide.
- **Événement à publier** (plus tard) : `CompteCree` (`account.created`).

## 7. Checklist
- [ ] entité + 2 enums + repository
- [ ] DTO (4 records)
- [ ] service (règles solde/plafond/découvert)
- [ ] controller (6 endpoints)
- [ ] `application.properties` (port 8082, base `bank_account_db`)
- [ ] base `bank_account_db` (déjà dans `init.sql`) + service déjà dans `docker-compose`
- [ ] test : ouvrir → créditer → débiter (dont cas 409)
