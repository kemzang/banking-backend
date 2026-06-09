# Guides d'implémentation des services

> Spec **explicite** par service : entités, attributs, DTO, endpoints et **ce que
> chaque controller doit retourner**. À suivre pour rester cohérent avec les
> services déjà faits (`auth-service`, `customer-service`).

## État des services

| Service | Techno | État | Guide |
|---------|--------|------|-------|
| discovery-service | Java | ✅ fait | — |
| config-service | Java | ✅ fait | — |
| gateway-service | Java | ✅ fait (sécurisé JWT) | — |
| auth-service | Java | ✅ fait | (modèle de référence sécurité) |
| ai-document-service | Python | ✅ fait (Daryl) | — |
| **customer-service** | Java | 🟡 partiel (à finir) | [customer-service.md](customer-service.md) |
| **account-service** | Java | 🔴 à faire | [account-service.md](account-service.md) |
| **transaction-service** | Java | 🔴 à faire | [transaction-service.md](transaction-service.md) |
| **loan-service** | Java | 🔴 à faire | [loan-service.md](loan-service.md) |
| **notification-service** | Node.js | 🔴 à faire | [notification-service.md](notification-service.md) |

## Le patron à respecter (services Java)

Chaque service Java suit **exactement** la structure de `customer-service` :
```
entity/        -> @Entity JPA + enums + @Embeddable
repository/    -> interface extends JpaRepository<Entity, Id>
dto/           -> records : XxxRequestDTO (entrée) + XxxResponseDTO (sortie)
service/       -> @Service @RequiredArgsConstructor : règles métier + mapping DTO<->Entity
controller/    -> @RestController @RequestMapping("/api/...") : endpoints REST
```

### Conventions (rappel)
- **Ne jamais exposer l'entité** directement → toujours via DTO (records).
- Les **références inter-services** sont des `Long`/`UUID` (ex : `clientId`), **jamais** un `@ManyToOne` vers un autre service.
- Enums stockés en texte : `@Enumerated(EnumType.STRING)`.
- Erreurs → `ResponseStatusException` (404 introuvable, 409 conflit, 400 invalide).
- Montants en `BigDecimal` (jamais `double` pour de l'argent).
- Codes HTTP : `201` création, `200` lecture/maj, `404`, `409`, `400`. (cf. [contracts](../contracts/README.md))

### Config type d'un service Java (`application.properties`)
```properties
server.port=${SERVER_PORT:808X}
spring.application.name=xxx-service
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/bank_xxx_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:bank}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:bank}
spring.jpa.hibernate.ddl-auto=update
eureka.client.service-url.defaultZone=${EUREKA_CLIENT_SERVICEURL_DEFAULTZONE:http://localhost:8761/eureka/}
```
> Ports : customer 8081, account 8082, transaction 8083, loan 8084, auth 8085.
> Pense à créer la base dans `infra/postgres/init.sql` et l'entrée dans `docker-compose.yml`.

## Communication entre services (IMPORTANT pour transaction & loan)

Certains services doivent **appeler** d'autres (ex : `transaction` doit créditer un
compte géré par `account`). Deux approches en synchrone :

### Option recommandée : `RestClient` + load balancing Eureka
```java
// Config (une fois)
@Configuration
public class HttpConfig {
    @Bean
    @LoadBalanced                      // permet d'utiliser le NOM du service comme hôte
    RestClient.Builder restClientBuilder() { return RestClient.builder(); }
}

// Usage dans un service
private final RestClient.Builder lbBuilder;
...
ResponseEntity<Void> r = lbBuilder.build()
    .post()
    .uri("http://account-service/api/accounts/{id}/debit", compteId)
    .body(new MontantDTO(montant))
    .retrieve()
    .toBodilessEntity();
```
- `http://account-service/...` : le nom Eureka est résolu automatiquement (pas d'IP).
- Dépendance nécessaire : `spring-cloud-starter-loadbalancer` (souvent déjà transitive).

### Communication asynchrone (événements)
Pour notifier sans bloquer (ex : « transaction effectuée » → notification), on publie
un événement RabbitMQ. Voir [contracts/02-evenements.md](../contracts/02-evenements.md).
**Conseil : faire d'abord marcher le synchrone (REST), ajouter les événements ensuite.**

## Ordre de réalisation conseillé
1. **Finir customer-service** (Operateur + endpoints manquants)
2. **account-service** (base des transactions)
3. **transaction-service** (appelle account)
4. **loan-service** (le plus complexe)
5. **notification-service** (consomme les événements)
