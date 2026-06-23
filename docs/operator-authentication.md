# Authentification des utilisateurs operateurs

## Modele

- L'organisation financiere reste dans `customer-service` (`operateur`).
- Le compte de connexion reste dans `auth-service` (`utilisateur`).
- `utilisateur.operator_id` contient l'identifiant de l'organisation distante.
- Les roles canoniques sont `CLIENT`, `ADMIN_PLATFORM`, `OPERATOR_ADMIN` et `OPERATOR_AGENT`.
- `operatorId` est obligatoire pour les deux roles operateur.

Les anciennes valeurs `ADMIN` et `OPERATEUR` restent lisibles le temps de migrer la base. Au demarrage, `ADMIN` devient `ADMIN_PLATFORM`; `OPERATEUR` devient `OPERATOR_AGENT` uniquement si la ligne possede deja un `operatorId`.

## Demarrage Docker

Depuis la racine du projet :

```powershell
docker compose up -d postgres rabbitmq
docker compose up -d --build config-service discovery-service
docker compose up -d --build customer-service auth-service gateway-service
docker compose up -d --build account-service transaction-service notification-service
docker compose up -d --build frontend-app
docker compose ps
```

URLs utiles :

- Gateway : `http://localhost:8080`
- Frontend : `http://localhost:4200`
- Eureka : `http://localhost:8761`
- RabbitMQ : `http://localhost:15672`

## Scenario Postman

Toutes les requetes metier passent par la gateway.

### 1. Connexion admin plateforme

`POST http://localhost:8080/api/auth/login`

```json
{
  "email": "admin@bank.cm",
  "password": "admin123"
}
```

Conserver `accessToken` (ou le champ compatible `token`) dans `adminToken`.

### 2. Creation d'une organisation

`POST http://localhost:8080/api/operators`

Header : `Authorization: Bearer {{adminToken}}`

```json
{
  "name": "Express Union",
  "code": "EXPRESS_UNION",
  "type": "MICROFINANCE",
  "status": "ACTIVE"
}
```

Conserver l'`id` retourne dans `operatorId`.

### 3. Creation d'un utilisateur operateur

`POST http://localhost:8080/api/auth/operator-users`

Header : `Authorization: Bearer {{adminToken}}`

```json
{
  "firstName": "Agent",
  "lastName": "Express",
  "email": "agent@expressunion.cm",
  "password": "Password123@",
  "role": "OPERATOR_AGENT",
  "operatorId": {{operatorId}}
}
```

`auth-service` appelle `GET /api/operators/{operatorId}` dans `customer-service`. Un identifiant absent renvoie `400`; un service indisponible renvoie `503`.

### 4. Connexion de l'agent

`POST http://localhost:8080/api/auth/login`

```json
{
  "email": "agent@expressunion.cm",
  "password": "Password123@"
}
```

La reponse contient le jeton et l'utilisateur :

```json
{
  "token": "...",
  "accessToken": "...",
  "type": "Bearer",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "email": "agent@expressunion.cm",
    "roles": ["OPERATOR_AGENT"],
    "operatorId": 1,
    "firstName": "Agent",
    "lastName": "Express"
  }
}
```

Le JWT contient `userId`, `sub` (email), `roles` et `operatorId`. La gateway remplace toute identite fournie par le client et propage `X-User-Id`, `X-User-Email`, `X-User-Roles` et `X-Operator-Id`.

### 5. Verification du profil

`GET http://localhost:8080/api/auth/me`

Header : `Authorization: Bearer {{operatorToken}}`

Le profil doit contenir le meme `operatorId` et le meme role que la reponse de login.

## Verification rapide

- [ ] L'organisation est creee avec le jeton `ADMIN_PLATFORM`.
- [ ] Un utilisateur operateur avec un `operatorId` inexistant est refuse.
- [ ] `CLIENT` est refuse par `/api/auth/operator-users`.
- [ ] L'agent peut se connecter sur `/auth/operator`.
- [ ] Le JWT contient `operatorId`.
- [ ] `/api/auth/me` retourne `operatorId`.
- [ ] Un agent ne voit pas les comptes d'un autre operateur.
- [ ] Une requete envoyant un faux `X-Operator-Id` ne contourne pas la gateway.
