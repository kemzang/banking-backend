# Diagrammes de séquence & de déploiement

Diagrammes Mermaid (s'affichent dans VSCode / GitHub).

## 1. Séquence — Authentification (mot de passe + JWT)
```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant FE as Frontend
    participant GW as Gateway
    participant AU as auth-service
    U->>FE: email + mot de passe
    FE->>GW: POST /api/auth/login
    GW->>AU: route (public, pas de JWT requis)
    AU->>AU: vérifie BCrypt
    AU-->>FE: { token JWT, expiresIn }
    Note over FE: stocke le token (localStorage)
    FE->>GW: GET /api/customers (Authorization: Bearer ...)
    GW->>GW: filtre JWT : vérifie signature
    GW->>AU: + en-têtes X-User-Email / X-User-Roles
```

## 2. Séquence — Connexion Google
```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant FE as Frontend
    participant G as Google
    participant GW as Gateway
    participant AU as auth-service
    U->>FE: clic "Se connecter avec Google"
    FE->>G: authentification Google
    G-->>FE: ID token (signé Google)
    FE->>GW: POST /api/auth/google { idToken }
    GW->>AU: route publique
    AU->>G: vérifie signature + audience (client-id)
    AU->>AU: crée l'utilisateur si absent
    AU-->>FE: notre JWT
```

## 3. Séquence — Transfert (synchrone + circuit breaker + événement async)
```mermaid
sequenceDiagram
    participant FE as Frontend
    participant GW as Gateway
    participant TX as transaction-service
    participant CB as CircuitBreaker
    participant ACC as account-service
    participant MQ as RabbitMQ
    participant NO as notification-service
    FE->>GW: POST /api/transactions/transfer
    GW->>TX: route (JWT vérifié)
    TX->>CB: debit(source)
    CB->>ACC: POST /accounts/{src}/debit
    ACC-->>TX: OK (ou 409 solde insuffisant)
    TX->>CB: credit(dest)
    CB->>ACC: POST /accounts/{dest}/credit
    ACC-->>TX: OK
    TX-->>FE: 201 Transaction VALIDEE
    TX-)MQ: publie transaction.completed
    MQ-)NO: livraison événement
    NO->>NO: enregistre + envoie email
```

## 4. Séquence — Prêt (demande → décision → échéancier → crédit compte)
```mermaid
sequenceDiagram
    actor CL as Client
    actor OP as Opérateur/Admin
    participant FE as Frontend
    participant GW as Gateway
    participant LO as loan-service
    participant ACC as account-service
    OP->>FE: soumet une demande pour le client
    FE->>GW: POST /api/loans/applications
    GW->>LO: route
    LO->>LO: calcule score de risque
    LO-->>FE: DemandePret SOUMISE (+ score risque)
    OP->>FE: approuve (taux, compte de versement)
    FE->>GW: POST /api/loans/applications/{id}/decision
    GW->>LO: route
    LO->>LO: crée Pret + génère échéancier (amortissement constant)
    LO->>ACC: POST /accounts/{compteId}/credit (montant du prêt)
    ACC-->>LO: solde mis à jour
    LO-->>FE: Pret ACTIF
    FE->>GW: GET /api/loans/{id}/schedule
    GW->>LO: route
    LO-->>FE: échéancier (mensualités)
    Note over CL,FE: Le client voit ses prêts dans son dashboard
    CL->>FE: consulte ses prêts
    FE->>GW: GET /api/loans?clientId={id}
    GW->>LO: route (filtré par clientId)
    LO-->>FE: liste des prêts du client
```

## 5. Séquence — OCR alimentant le KYC
```mermaid
sequenceDiagram
    actor OP as Opérateur
    participant FE as Frontend
    participant GW as Gateway
    participant AI as ai-document (Python)
    participant CU as customer-service
    OP->>FE: choisit client + type, téléverse image
    FE->>GW: POST /api/v1/ocr/extract (multipart)
    GW->>AI: route (URL directe)
    AI->>AI: OCR Tesseract
    AI-->>FE: texte extrait + score
    OP->>FE: "Valider le KYC"
    FE->>GW: PATCH /api/customers/{id}/kyc {VALIDE}
    GW->>CU: route
    CU-->>FE: client KYC = VALIDE
```

## 6. Diagramme de déploiement (conteneurs)
```mermaid
flowchart TB
    subgraph Host/Cluster
      direction TB
      FE["frontend-app (Nginx) :4200"]
      GW["gateway-service :8080"]
      subgraph Socle
        CFG["config-service :8888"]
        EUR["discovery-service :8761"]
      end
      AU["auth-service :8085"]
      CU["customer-service :8081"]
      AC["account-service :8082"]
      TX["transaction-service :8083"]
      LO["loan-service :8084"]
      AI["ai-document-service :8001"]
      NO["notification-service :3000"]
      PG[("PostgreSQL :5432")]
      MQ[("RabbitMQ :5672")]
      PR["Prometheus :9090"]
      GF["Grafana :3001"]
    end
    FE --> GW
    GW --> AU & CU & AC & TX & LO & AI & NO
    AU & CU & AC & TX & LO --> PG
    TX --> MQ --> NO
    AU & CU & AC & TX & LO --> EUR
    EUR --> CFG
    PR --> GW & AU & AC & TX & LO
    GF --> PR
```

> Réseau Docker `bank-net` (ou namespace K8s `banking`). Chaque service Java possède
> sa base logique dans l'instance PostgreSQL (database per service).
