# customer-service — à FINIR

**Port** 8081 · **Base** `bank_customer_db` · **Package** `com.banking.customer_service`

Déjà fait ✅ : entité `Client` (+ `Adresse`, enums), `ClientRepository`, DTO,
`ClientService` (créer/lire), `ClientController` (`POST` + `GET /{id}`).

Reste à faire : **l'entité `Operateur`** (CRUD) et **les endpoints `Client` manquants**.

---

## A. Entité `Operateur` (nouveau, package `entity`)

| Attribut | Type | Notes |
|----------|------|-------|
| id | `Long` | `@Id @GeneratedValue(IDENTITY)` |
| nom | `String` | `@Column(nullable=false)` |
| type | `TypeOperateur` | `@Enumerated(STRING)` |
| code | `String` | `@Column(unique=true)` |

```java
public enum TypeOperateur { BANQUE, MICROFINANCE, MOBILE }
```

**Repository :**
```java
public interface OperateurRepository extends JpaRepository<Operateur, Long> {
    boolean existsByCode(String code);
}
```

**DTO :**
```java
public record OperateurRequestDTO(String nom, TypeOperateur type, String code) {}
public record OperateurResponseDTO(Long id, String nom, TypeOperateur type, String code) {}
```

**Service `OperateurService`** : `creer` (409 si `code` existe), `getById` (404), `lister`.

**Controller `@RequestMapping("/api/operators")`**

| Méthode | Chemin | Entrée | Retour | Code |
|---------|--------|--------|--------|------|
| POST | `/api/operators` | `OperateurRequestDTO` | `OperateurResponseDTO` | **201** |
| GET | `/api/operators` | — | `List<OperateurResponseDTO>` | 200 |
| GET | `/api/operators/{id}` | — | `OperateurResponseDTO` | 200 (404) |

---

## B. Endpoints `Client` manquants (à ajouter au `ClientController` existant)

| Méthode | Chemin | Entrée | Retour | Code |
|---------|--------|--------|--------|------|
| GET | `/api/customers` | — | `List<ClientResponseDTO>` (ou page) | 200 |
| PUT | `/api/customers/{id}` | `ClientRequestDTO` | `ClientResponseDTO` | 200 (404) |
| PATCH | `/api/customers/{id}/kyc` | `KycRequestDTO` | `ClientResponseDTO` | 200 |

```java
public record KycRequestDTO(StatutKyc statutKyc) {}   // ex. { "statutKyc": "VALIDE" }
```

**Méthodes `ClientService` à ajouter :**
- `lister()` → `clientRepository.findAll().stream().map(this::toResponse).toList()`.
- `modifier(id, dto)` → `findById(404)`, mettre à jour les champs autorisés, `save`.
- `majKyc(id, statut)` → `findById(404)`, `client.setStatutKyc(statut)`, `save`.
  (Plus tard : déclenché par l'événement `document.verified`.)

**Exemple de retour `PATCH /api/customers/5/kyc` :**
```json
// body: { "statutKyc": "VALIDE" }  -> 200
{ "id": 5, "nom": "Doe", "prenom": "Jean", "email": "jean@mail.com",
  "statutKyc": "VALIDE", "operateurId": 1 }
```

---

## C. (Optionnel) Validation des entrées
Ajouter `spring-boot-starter-validation`, annoter les records (`@NotBlank`, `@Email`,
`@Positive`…) et mettre `@Valid` devant les `@RequestBody` → renvoie `400` si invalide.

## D. Checklist — ✅ FAIT (9 juin 2026)
- [x] entité `Operateur` + enum `TypeOperateur` + repository + DTO + service + controller (`/api/operators`)
- [x] endpoints Client : `GET` liste, `PUT`, `PATCH /kyc` (+ DTO `KycRequestDTO`)
- [ ] (option) validation `@Valid` — non fait (reste optionnel)
- [x] **Gateway** : route `/api/operators/**` → `lb://customer-service`
- [x] test via gateway : opérateur créé (201) → client créé (201) → modifié (200) → KYC VALIDE (200)
