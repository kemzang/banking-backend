# Loan Service - Service de Prêts

**Port**: 8084 • **Base**: `bank_loan_db` • **Package**: `com.banking.loan_service`

Service gérant les demandes de prêt, décisions, génération d'échéanciers et remboursements.

## 🏗️ Architecture

### Entités
- **DemandePret**: Demandes de prêt soumises par les clients
- **Pret**: Prêts approuvés avec échéancier
- **Echeance**: Lignes de l'échéancier (relation interne autorisée)
- **Remboursement**: Historique des paiements

### Enums
- `StatutDemande`: SOUMISE, EN_ANALYSE, APPROUVEE, REJETEE
- `StatutPret`: ACTIF, SOLDE, EN_DEFAUT
- `StatutEcheance`: A_PAYER, PAYEE, EN_RETARD
- `MoyenPaiement`: COMPTE, MOBILE

## 🔄 Workflow

1. **Soumission**: Client soumet une demande → Status SOUMISE + calcul score risque
2. **Analyse**: Décision APPROUVEE/REJETEE
3. **Si approuvé**: Création du prêt + génération échéancier + crédit compte
4. **Remboursements**: Paiement des échéances jusqu'au solde complet

## 📊 Génération d'Échéancier

### Algorithme (Amortissement à mensualités constantes)

```
t = tauxInteret / 12                      // taux mensuel
n = dureeMois
mensualite = capital * t / (1 - (1 + t)^(-n))   // si t > 0, sinon capital/n

Pour chaque mois i de 1 à n :
  interet_i  = capitalRestant * t
  capital_i  = mensualite - interet_i
  montantTotal_i = mensualite
  capitalRestant -= capital_i
  dateEcheance_i = dateDeblocage + i mois
```

## 🌐 API Endpoints

| Méthode | Endpoint | Description | Code |
|---------|----------|-------------|------|
| POST | `/api/loans/applications` | Soumettre demande | 201 |
| GET | `/api/loans/applications/{id}` | Consulter demande | 200/404 |
| POST | `/api/loans/applications/{id}/decision` | Prendre décision | 200 |
| GET | `/api/loans/{id}` | Consulter prêt | 200/404 |
| GET | `/api/loans/{id}/schedule` | Voir échéancier | 200 |
| POST | `/api/loans/{id}/repay` | Rembourser | 200 |

### Exemples

#### Soumettre une demande
```bash
curl -X POST http://localhost:8084/api/loans/applications \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": 1,
    "montantDemande": 1000000.00,
    "dureeMois": 12,
    "motif": "Achat véhicule"
  }'
```

#### Approuver une demande
```bash
curl -X POST http://localhost:8084/api/loans/applications/1/decision \
  -H "Content-Type: application/json" \
  -d '{
    "approuver": true,
    "tauxInteret": 0.12,
    "compteId": 1
  }'
```

#### Consulter l'échéancier
```bash
curl http://localhost:8084/api/loans/1/schedule
```

## 🚀 Démarrage

### Prérequis
- Java 17+
- PostgreSQL (base `bank_loan_db`)
- Kafka (événements)
- Eureka (découverte de services)

### Configuration
```yml
server:
  port: 8084
  
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bank_loan_db
  
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
```

### Build & Run
```bash
# Build
./mvnw clean package -DskipTests

# Run local
./mvnw spring-boot:run

# Run avec Docker
docker build -t loan-service .
docker run -p 8084:8084 loan-service
```

### Tests
```bash
# Tests unitaires
./mvnw test

# Tests d'intégration
./mvnw test -Dtest=LoanWorkflowTest
```

## 🔗 Intégrations

- **Gateway**: Routage via `lb://loan-service`
- **Account-Service**: Crédit du compte au déblocage (optionnel)
- **Events**: Publication `PretApprouve`, `PretRejete`, `EcheanceImpayee`

## 📈 Monitoring

- Health: `http://localhost:8084/actuator/health`
- Metrics: `http://localhost:8084/actuator/metrics`
- Info: `http://localhost:8084/actuator/info`