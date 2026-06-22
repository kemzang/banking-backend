# Patterns de conception par service

Ce document recense les principaux design patterns utilisés dans chaque service de la plateforme banking, accompagnés d'un schéma d'implémentation et d'un extrait de code réel.

---

## 1. Gateway Service (Java / Spring Cloud Gateway)

### 1.1 Chain of Responsibility — Filtres en chaîne

```
Requête entrante
      │
      ▼
┌─────────────────────┐  order=-1
│ JwtAuthenticationFilter │──► 401 si token invalide
└──────────┬──────────┘
           │ token OK → propage X-User-Email / X-User-Roles
           ▼
┌─────────────────────┐  order=0
│  AuditLoggingFilter │──► log(method, path, user, status, ms)
└──────────┬──────────┘
           │
           ▼
     Service cible
```

**Code — `JwtAuthenticationFilter.java`**
```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/google");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) return chain.filter(exchange); // laisse passer

        String authHeader = exchange.getRequest().getHeaders()
                                    .getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return unauthorized(exchange);

        Claims claims = parseToken(authHeader.substring(7));
        // Propage l'identité aux services en aval
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Email", claims.getSubject())
                .header("X-User-Roles", String.join(",", (List<?>)claims.get("roles")))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override public int getOrder() { return -1; } // s'exécute avant AuditLoggingFilter
}
```

**Code — `AuditLoggingFilter.java`**
```java
@Component
public class AuditLoggingFilter implements GlobalFilter, Ordered {
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String user = exchange.getRequest().getHeaders().getFirst("X-User-Email");
        long start  = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            AUDIT.info("method={} path={} user={} status={} dureeMs={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    user != null ? user : "anonymous",
                    exchange.getResponse().getStatusCode(),
                    System.currentTimeMillis() - start);
        }));
    }

    @Override public int getOrder() { return 0; } // après JwtAuthenticationFilter
}
```

---

## 2. Auth Service (Java / Spring Boot)

### 2.1 Strategy — Authentification email vs Google OAuth

```
         AuthService
              │
       ┌──────┴───────┐
       │               │
  LoginRequest    GoogleLoginRequest
       │               │
 PasswordEncoder  GoogleTokenVerifier
 (BCrypt strategy)  (Google OAuth2 strategy)
       │               │
       └──────┬────────┘
              │
         JwtService.generateToken()
              │
         JWT renvoyé au client
```

**Code — `AuthService.java` (les deux stratégies)**
```java
// Stratégie 1 : email + mot de passe
public AuthResponse login(LoginRequest req) {
    Utilisateur u = utilisateurRepository.findByEmail(req.email())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));
    if (!passwordEncoder.matches(req.motDePasse(), u.getMotDePasse()))
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
    return buildAuthResponse(u);
}

// Stratégie 2 : Google OAuth2
public AuthResponse googleLogin(String idToken) {
    String email = googleTokenVerifier.verifyAndGetEmail(idToken); // délégation
    Utilisateur u = utilisateurRepository.findByEmail(email)
            .orElseGet(() -> creerUtilisateurGoogle(email));
    return buildAuthResponse(u);
}
```

**Code — `GoogleTokenVerifier.java` (Adapter vers le SDK Google)**
```java
@Component
public class GoogleTokenVerifier {
    @Value("${app.google.client-id:}") private String clientId;

    public String verifyAndGetEmail(String idTokenString) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Jeton Google invalide.");
        return idToken.getPayload().getEmail(); // email extrait et retourné
    }
}
```

---

## 3. Account Service (Java / Spring Boot)

### 3.1 Repository Pattern

```
CompteService
     │
     │ appelle
     ▼
CompteRepository  ──extends──►  JpaRepository<Compte, Long>
     │                                    │
     │ méthodes métier custom             │ CRUD générique
     ▼                                    ▼
findByClientId()                  save() / findById() / ...
findByNumeroCompte()
existsByNumeroCompte()
countByOperateurId()
```

**Code — `CompteRepository.java`**
```java
public interface CompteRepository extends JpaRepository<Compte, Long> {
    List<Compte> findByClientId(Long clientId);
    Optional<Compte> findByNumeroCompte(String numeroCompte);
    boolean existsByNumeroCompte(String numeroCompte);
    long countByOperateurId(Long operateurId);
}
```

**Code — `CompteService.java` (utilisation du repository)**
```java
@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository; // injecté

    @Transactional
    public CompteResponseDTO ouvrirCompte(CompteRequestDTO dto) {
        Compte compte = Compte.builder()
                .clientId(dto.clientId())
                .type(dto.type())
                .devise(dto.devise() != null ? dto.devise() : "XAF")
                .solde(BigDecimal.ZERO)
                .statut(StatutCompte.ACTIF)
                .build();
        return toResponse(compteRepository.save(compte)); // persistance déléguée
    }

    @Transactional
    public CompteResponseDTO cloturer(Long id) {
        Compte c = compteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable"));
        if (c.getSolde().compareTo(BigDecimal.ZERO) != 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solde non nul");
        c.setStatut(StatutCompte.CLOTURE);
        return toResponse(compteRepository.save(c));
    }
}
```

---

## 4. Transaction Service (Java / Spring Boot)

### 4.1 Strategy — Calcul de commission

```
TransactionService
        │
        │ injecte
        ▼
 <<interface>>
CommissionStrategy
        │
        │ implémenté par
        ▼
PercentageCommissionStrategy
  ├── standardRate     (0.5% par défaut)
  └── sameOperatorRate (0% si même opérateur)
```

**Code — `CommissionStrategy.java` (interface)**
```java
public interface CommissionStrategy {
    BigDecimal calculate(BigDecimal montant, Long operateurSourceId, Long operateurDestId);
}
```

**Code — `PercentageCommissionStrategy.java` (implémentation concrète)**
```java
@Component
public class PercentageCommissionStrategy implements CommissionStrategy {
    private final BigDecimal standardRate;
    private final BigDecimal sameOperatorRate;

    public PercentageCommissionStrategy(
            @Value("${transaction.commission.percentage:0.005}") BigDecimal standardRate,
            @Value("${transaction.commission.same-operator-percentage:0}") BigDecimal sameOperatorRate) {
        this.standardRate     = standardRate;
        this.sameOperatorRate = sameOperatorRate;
    }

    @Override
    public BigDecimal calculate(BigDecimal montant, Long srcOp, Long dstOp) {
        boolean memeOperateur = srcOp != null && srcOp.equals(dstOp);
        BigDecimal rate = memeOperateur ? sameOperatorRate : standardRate;
        return montant.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
```

---

### 4.2 Factory — Création des événements de domaine

```
TransactionService
        │ appelle
        ▼
TransactionEventFactory  (static factory, constructeur privé)
  ├── completed(tx)   ──► TransactionCompletedEvent
  ├── rejected(tx)    ──► TransactionRejectedEvent
  └── compensation()  ──► CompensationRequestedEvent
        │
        ▼
TransactionEventPublisher.publish(event)
```

**Code — `TransactionEventFactory.java`**
```java
final class TransactionEventFactory {
    private TransactionEventFactory() {} // non instanciable

    static TransactionCompletedEvent completed(Transaction tx) {
        return new TransactionCompletedEvent(
                "transaction.completed",
                tx.getId(), tx.getReference(), tx.getType(),
                tx.getCompteSourceId(), tx.getCompteDestId(),
                tx.getMontant(), tx.getCommission(), tx.getDevise(),
                tx.getDateOperation());
    }

    static TransactionRejectedEvent rejected(Transaction tx) {
        return new TransactionRejectedEvent(
                "transaction.rejected",
                tx.getId(), tx.getReference(), tx.getType(),
                tx.getCompteSourceId(), tx.getCompteDestId(),
                tx.getMontant(), tx.getDevise(), tx.getMotif(),
                tx.getDateOperation());
    }
}
```

---

### 4.3 Circuit Breaker — Résilience vers account-service

```
TransactionService
        │
        ▼
  RestAccountClient
        │
        ▼
  CircuitBreaker ("account")
        │
   ┌────┴────┐
   │ CLOSED  │ ──► appel HTTP normal vers account-service
   └────┬────┘
        │ trop d'erreurs
        ▼
   ┌────────┐
   │  OPEN  │ ──► fallback immédiat (503 ServiceUnavailable)
   └────────┘
        │ après délai
        ▼
   ┌──────────┐
   │ HALF-OPEN│ ──► teste une requête, repasse CLOSED si OK
   └──────────┘
```

**Code — `RestAccountClient.java`**
```java
@Component
public class RestAccountClient implements AccountClient {
    private final CircuitBreaker circuitBreaker;

    public RestAccountClient(RestClient accountRestClient,
                             CircuitBreakerFactory<?, ?> factory) {
        this.circuitBreaker = factory.create("account");
    }

    @Override
    public AccountResponseDTO getById(Long accountId) {
        // Protégé : si account-service tombe, le circuit s'ouvre et on échoue vite
        return circuitBreaker.run(() -> doGetById(accountId), this::fallback);
    }

    @Override
    public void debit(Long accountId, BigDecimal amount) {
        circuitBreaker.run(() -> { updateBalance(accountId, amount, "debit"); return null; },
                           t -> fallback(t));
    }

    private <T> T fallback(Throwable t) {
        throw new AccountServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "account-service est indisponible (circuit ouvert)", t);
    }
}
```

---

### 4.4 Observer (Event-Driven) — Publication vers RabbitMQ

```
TransactionService
        │ publie via interface
        ▼
<<interface>> TransactionEventPublisher
        │
        │ @ConditionalOnProperty(broker=rabbitmq)
        ▼
RabbitTransactionEventPublisher
        │
        ▼
RabbitTemplate.convertAndSend(exchange, routingKey, payload)
        │
        ▼
   [banking.events]  ──topic──►  transaction.completed
                                 transaction.rejected
                                 transaction.compensation.requested
```

**Code — `TransactionEventPublisher.java` (interface)**
```java
public interface TransactionEventPublisher {
    void publishTransactionCompleted(Transaction transaction);
    void publishTransactionRejected(Transaction transaction);
    void publishCompensationRequested(Transaction transaction, Long compteId, BigDecimal montant);
}
```

**Code — `RabbitTransactionEventPublisher.java` (implémentation)**
```java
@Service
@ConditionalOnProperty(name = "transaction.events.broker", havingValue = "rabbitmq")
public class RabbitTransactionEventPublisher implements TransactionEventPublisher {

    @Override
    public void publishTransactionCompleted(Transaction tx) {
        envoyer(topicCompleted, payload(tx));
    }

    private void envoyer(String routingKey, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(RabbitEventConfig.EXCHANGE, routingKey, json);
        } catch (Exception e) {
            LOGGER.error("[Event] Echec publication {} : {}", routingKey, e.getMessage());
        }
    }
}
```

---

## 5. Notification Service (Node.js)

### 5.1 Observer / Message Consumer — Abonnement RabbitMQ

```
   RabbitMQ Exchange "banking.events"
           │
           │ routing_key: transaction.completed
           ▼
   Queue "transaction.notifications"
           │
           ▼
   channel.consume(QUEUE_NOTIF, handler)
           │
           ▼
   handleTransactionEvent(event, transporter)
     ├── console.log(...)     ← toujours actif
     └── transporter.sendMail(...) ← si SMTP disponible
```

**Code — `rabbitmq.js` (connexion avec retry)**
```javascript
async function connect(attempt = 1) {
  try {
    const conn    = await amqp.connect(RABBITMQ_URL);
    const channel = await conn.createChannel();

    await channel.assertExchange('banking.events', 'topic', { durable: true });
    await channel.assertQueue('transaction.notifications', { durable: true });
    await channel.bindQueue('transaction.notifications', 'banking.events', 'transaction.completed');
    channel.prefetch(1); // traitement séquentiel

    // Reconnexion automatique si la connexion se ferme
    conn.on('close', () => setTimeout(() => connect(1), 5000));
    return channel;
  } catch (err) {
    if (attempt < MAX_RETRIES) {
      await new Promise(r => setTimeout(r, 5000));
      return connect(attempt + 1);
    }
    throw err;
  }
}
```

**Code — `transactionHandler.js` (traitement de l'événement)**
```javascript
async function handleTransactionEvent(event, transporter) {
  const { reference, type, montant, devise, statut } = event;

  // Log toujours actif
  console.log(`[Notification] ${type} | ${reference} | ${montant} ${devise} | ${statut}`);

  // Email envoyé si le transporter est disponible (dégradé sinon)
  const { sujet, html } = buildEmail(event);
  await transporter.sendMail({
    from: `"BankingApp" <${process.env.SMTP_USER}>`,
    to:   process.env.NOTIFICATION_EMAIL || 'admin@bankingapp.cm',
    subject: sujet,
    html,
  });
}
```

---

### 5.2 Template Method — Génération d'emails

```
buildEmail(event)
     │
     ├── montantFmt  (Intl.NumberFormat)
     ├── libelles[]  (map type → label)
     ├── icones[]    (map statut → emoji)
     ├── ligneComptes (varie selon DEPOT / RETRAIT / TRANSFERT)
     └── html template ──► { sujet, html }
```

**Code — `email.js`**
```javascript
function buildEmail(event) {
  const { reference, type, montant, devise, statut, motif } = event;

  const libelles = { DEPOT: 'Dépôt', RETRAIT: 'Retrait', TRANSFERT: 'Transfert' };
  const icones   = { SUCCES: '✅', ECHEC: '❌', EN_COURS: '⏳' };

  // Lignes variables selon le type d'opération
  const ligneComptes = type === 'TRANSFERT'
    ? `<tr><td>Compte source</td><td>#${event.compteSourceId}</td></tr>
       <tr><td>Compte destination</td><td>#${event.compteDestId}</td></tr>`
    : type === 'DEPOT'
    ? `<tr><td>Compte crédité</td><td>#${event.compteDestId}</td></tr>`
    : `<tr><td>Compte débité</td><td>#${event.compteSourceId}</td></tr>`;

  const sujet = `${icones[statut]} [BankingApp] ${libelles[type]} — ${reference}`;
  return { sujet, html: `...${ligneComptes}...` };
}
```

---

## 6. AI Document Service (Python / FastAPI)

### 6.1 Repository Pattern

```
Router (FastAPI)
     │
     │ depends(get_db)
     ▼
DocumentRepository(db: Session)
  ├── create(filename, text, score)
  ├── find_all()
  └── find_by_id(id)
     │
     ▼
SQLAlchemy ORM ──► SQLite / PostgreSQL
```

**Code — `document_repository.py`**
```python
class DocumentRepository:
    def __init__(self, db: Session) -> None:
        self.db = db  # session injectée

    def create(self, original_filename, stored_filename,
               extracted_text=None, confidence_score=0.0, status="completed"):
        analysis = DocumentAnalysis(
            original_filename=original_filename,
            stored_filename=stored_filename,
            extracted_text=extracted_text,
            confidence_score=confidence_score,
            status=status,
        )
        try:
            self.db.add(analysis)
            self.db.commit()
            self.db.refresh(analysis)
        except Exception:
            self.db.rollback()
            raise
        return analysis

    def find_all(self) -> list[DocumentAnalysis]:
        return list(self.db.scalars(
            select(DocumentAnalysis).order_by(DocumentAnalysis.created_at.desc())
        ).all())
```

### 6.2 Service Layer — Séparation logique métier

```
Router FastAPI  ──► appelle ──►  AnalysisService
                                      │
                                      │ logique pure (sans I/O)
                                      ▼
                              analyse statistique des valeurs
                              (count, average, min, max)
```

**Code — `analysis_service.py`**
```python
class AnalysisService:
    """Contient les règles métier pour l'analyse numérique — sans dépendance I/O."""

    def analyze_values(self, values: list[float]) -> dict:
        if not values:
            return {"count": 0, "average": 0.0, "minimum": 0.0, "maximum": 0.0}
        return {
            "count": len(values),
            "average": sum(values) / len(values),
            "minimum": min(values),
            "maximum": max(values),
        }
```

---

## 7. Frontend App (Angular / TypeScript)

### 7.1 Interceptor — Injection automatique du token JWT

```
Composant Angular
     │ HttpClient.post(...)
     ▼
authInterceptor (HttpInterceptorFn)
     │
     ├── AuthService.token ──► lit localStorage
     │
     ├── si token → req.clone({ Authorization: "Bearer ..." })
     │
     ▼
Requête HTTP sortante (avec header injecté)
```

**Code — `auth.interceptor.ts`**
```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).token;
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
```

---

### 7.2 Guard + Signal — Contrôle d'accès réactif

```
Router Angular
     │ navigate vers route protégée
     ▼
authGuard()
  └── AuthService.connecte() ──► Signal<boolean>
        │
    ┌───┴────┐
    │ true   │──► accès autorisé
    │ false  │──► redirect /login
    └────────┘

roleGuard()
  └── AuthService.hasRole(...requiredRoles)
        │ décode le JWT (claim "roles")
    ┌───┴────┐
    │ true   │──► accès autorisé
    │ false  │──► redirect /dashboard
    └────────┘
```

**Code — `auth.guard.ts`**
```typescript
export const authGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  if (auth.connecte()) return true;
  router.navigate(['/login']);
  return false;
};

export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  const requis = (route.data?.['roles'] as string[]) ?? [];
  if (requis.length === 0 || auth.hasRole(...requis)) return true;
  router.navigate(['/dashboard']);
  return false;
};
```

**Code — `auth.service.ts` (Signal réactif)**
```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  // Signal : la UI se met à jour automatiquement quand l'état change
  readonly connecte = signal<boolean>(this.hasToken());

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, req).pipe(
      tap((res) => {
        localStorage.setItem(this.TOKEN_KEY, res.token);
        this.connecte.set(true); // notifie tous les abonnés
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.connecte.set(false);
  }
}
```

---

## Récapitulatif

| Service | Patterns principaux |
|---|---|
| Gateway Service | Chain of Responsibility, Decorator, Facade |
| Auth Service | Strategy (multi-auth), Adapter (Google SDK), Repository |
| Account Service | Repository, Service Layer, DTO |
| Transaction Service | Strategy (commission), Factory (events), Circuit Breaker, Observer (Event-Driven) |
| Notification Service | Observer / Message Consumer, Template Method, Retry |
| AI Document Service | Repository, Service Layer, Dependency Injection |
| Frontend App | Interceptor, Guard, Signal (reactive state), Facade (services) |
