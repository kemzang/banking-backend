# loan-service — Prêts, échéancier, remboursements

**Port** 8084 · **Base** `bank_loan_db` · **Package** `com.banking.loan_service`
Rôle : demandes de prêt, décision, génération de l'échéancier, remboursements.
**C'est le service le plus riche** (4 entités liées). Prends le temps sur l'échéancier.

---

## 1. Entités (package `entity`)

### `DemandePret`
| Attribut | Type | Notes |
|----------|------|-------|
| id | `Long` | PK |
| clientId | `Long` | réf. customer |
| montantDemande | `BigDecimal` | |
| dureeMois | `int` | |
| motif | `String` | |
| scoreRisque | `BigDecimal` | calculé (placeholder, ou via ai-document) |
| statut | `StatutDemande` | `@Enumerated(STRING)` |
| dateSoumission | `LocalDateTime` | `@PrePersist` (statut=SOUMISE) |

### `Pret` (créé quand la demande est APPROUVEE)
| Attribut | Type | Notes |
|----------|------|-------|
| id | `Long` | PK |
| demandeId | `Long` | réf. DemandePret (1–1) |
| clientId | `Long` | |
| compteId | `Long` | compte de versement |
| montantAccorde | `BigDecimal` | |
| tauxInteret | `BigDecimal` | taux annuel (ex. 0.12) |
| dureeMois | `int` | |
| capitalRestant | `BigDecimal` | = montantAccorde au départ |
| statut | `StatutPret` | |
| dateDeblocage | `LocalDateTime` | |
| echeances | `List<Echeance>` | `@OneToMany(cascade=ALL, mappedBy="pret")` |

### `Echeance` (ligne de l'échéancier — table DANS loan-service)
| Attribut | Type | Notes |
|----------|------|-------|
| id | `Long` | PK |
| pret | `Pret` | `@ManyToOne` (relation INTERNE, autorisée car même service) |
| numero | `int` | 1..n |
| dateEcheance | `LocalDate` | |
| montantCapital | `BigDecimal` | |
| montantInteret | `BigDecimal` | |
| montantTotal | `BigDecimal` | capital + intérêt |
| statut | `StatutEcheance` | |

### `Remboursement`
| Attribut | Type | Notes |
|----------|------|-------|
| id | `Long` | PK |
| echeanceId | `Long` | réf. interne |
| montant | `BigDecimal` | |
| datePaiement | `LocalDateTime` | |
| moyenPaiement | `MoyenPaiement` | |

**Enums :**
```java
public enum StatutDemande { SOUMISE, EN_ANALYSE, APPROUVEE, REJETEE }
public enum StatutPret { ACTIF, SOLDE, EN_DEFAUT }
public enum StatutEcheance { A_PAYER, PAYEE, EN_RETARD }
public enum MoyenPaiement { COMPTE, MOBILE }
```
> Note : ici `@ManyToOne`/`@OneToMany` entre `Pret` et `Echeance` est **autorisé**
> (même service, même base). L'interdiction ne concerne QUE les liens **inter-services**.

## 2. Repositories
```java
public interface DemandePretRepository extends JpaRepository<DemandePret, Long> {}
public interface PretRepository extends JpaRepository<Pret, Long> {}
public interface EcheanceRepository extends JpaRepository<Echeance, Long> {}
public interface RemboursementRepository extends JpaRepository<Remboursement, Long> {}
```

## 3. DTO (records)
```java
public record DemandePretRequestDTO(Long clientId, BigDecimal montantDemande, int dureeMois, String motif) {}
public record DemandePretResponseDTO(Long id, Long clientId, BigDecimal montantDemande,
                                     int dureeMois, BigDecimal scoreRisque, StatutDemande statut) {}
public record DecisionRequestDTO(boolean approuver, BigDecimal tauxInteret, Long compteId, String motifRejet) {}
public record PretResponseDTO(Long id, Long clientId, BigDecimal montantAccorde,
                              BigDecimal tauxInteret, int dureeMois, BigDecimal capitalRestant, StatutPret statut) {}
public record EcheanceDTO(int numero, LocalDate dateEcheance, BigDecimal montantCapital,
                          BigDecimal montantInteret, BigDecimal montantTotal, StatutEcheance statut) {}
public record EcheancierDTO(Long pretId, List<EcheanceDTO> echeances) {}
public record RemboursementRequestDTO(BigDecimal montant, MoyenPaiement moyenPaiement) {}
```

## 4. Service — logique

- **soumettre(dto)** : crée `DemandePret` `SOUMISE` (calcule un `scoreRisque` simple, ex. selon montant/durée).
- **decider(id, decision)** :
  - si `approuver=false` → demande `REJETEE` (publier `PretRejete`).
  - si `approuver=true` → demande `APPROUVEE`, créer `Pret` (capitalRestant=montant), **générer l'échéancier**, (idéalement) appeler account-service pour **créditer** le compte du montant, publier `PretApprouve`.
- **getEcheancier(pretId)** : renvoie les échéances triées par `numero`.
- **rembourser(pretId, dto)** : marque la prochaine échéance `A_PAYER` comme `PAYEE`, diminue `capitalRestant` ; si toutes payées → `Pret.statut = SOLDE`.

### 📐 Génération de l'échéancier (amortissement à mensualités constantes)
```
t = tauxInteret / 12                      // taux mensuel
n = dureeMois
mensualite = capital * t / (1 - (1 + t)^(-n))   // si t > 0, sinon capital/n
```
Pour chaque mois `i` de 1 à n :
```
interet_i  = capitalRestant * t
capital_i  = mensualite - interet_i
montantTotal_i = mensualite
capitalRestant -= capital_i
dateEcheance_i = dateDeblocage + i mois
```
(Utiliser `BigDecimal` avec un arrondi `setScale(2, HALF_UP)`.)

## 5. Controller `@RequestMapping("/api/loans")`

| Méthode | Chemin | Entrée | Retour | Code |
|---------|--------|--------|--------|------|
| POST | `/api/loans/applications` | `DemandePretRequestDTO` | `DemandePretResponseDTO` | **201** |
| GET | `/api/loans/applications/{id}` | — | `DemandePretResponseDTO` | 200 (404) |
| POST | `/api/loans/applications/{id}/decision` | `DecisionRequestDTO` | `PretResponseDTO` (ou demande rejetée) | 200 |
| GET | `/api/loans/{id}` | — | `PretResponseDTO` | 200 (404) |
| GET | `/api/loans/{id}/schedule` | — | `EcheancierDTO` | 200 |
| POST | `/api/loans/{id}/repay` | `RemboursementRequestDTO` | `PretResponseDTO` | 200 |

**Exemples de retour :**
```json
// POST /api/loans/applications -> 201
{ "id": 30, "clientId": 5, "montantDemande": 1000000.00, "dureeMois": 12,
  "scoreRisque": 0.30, "statut": "SOUMISE" }

// GET /api/loans/40/schedule -> 200
{ "pretId": 40, "echeances": [
  { "numero": 1, "dateEcheance": "2026-07-08", "montantCapital": 80501.00,
    "montantInteret": 10000.00, "montantTotal": 90501.00, "statut": "A_PAYER" }
]}
```

## 6. Intégration
- **Gateway** : `lb://loan-service` sur `/api/loans/**` (déjà présente).
- **Appelle** (optionnel) `account-service` (créditer le compte au déblocage).
- **Écoute** (plus tard) `document.verified`, **publie** `PretApprouve`, `EcheanceImpayee`.

## 7. Checklist
- [ ] 4 entités + 4 enums (relation interne Pret–Echeance) + repositories
- [ ] DTO (8 records)
- [ ] service : soumission, décision, **génération échéancier**, remboursement
- [ ] controller (6 endpoints)
- [ ] config (port 8084, base `bank_loan_db`)
- [ ] test : soumettre → approuver → consulter échéancier → rembourser
