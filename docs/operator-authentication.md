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

### 3. Creation du premier OPERATOR_ADMIN

`POST http://localhost:8080/api/auth/operator-admins`

Header : `Authorization: Bearer {{adminToken}}`

```json
{
  "firstName": "Admin",
  "lastName": "Express",
  "email": "admin.operator@expressunion.cm",
  "password": "Password123@",
  "operatorId": {{operatorId}}
}
```

`auth-service` appelle `GET /api/operators/{operatorId}` dans `customer-service`. Un identifiant absent renvoie `400`; un service indisponible renvoie `503`.

### 4. Connexion de l'administrateur operateur

`POST http://localhost:8080/api/auth/login`

```json
{
  "email": "admin.operator@expressunion.cm",
  "password": "Password123@",
  "loginType": "OPERATOR_LOGIN"
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
    "email": "admin.operator@expressunion.cm",
    "roles": ["OPERATOR_ADMIN"],
    "operatorId": 1,
    "firstName": "Admin",
    "lastName": "Express"
  }
}
```

Le JWT contient `userId`, `sub` (email), `roles` et `operatorId`. La gateway remplace toute identite fournie par le client et propage `X-User-Id`, `X-User-Email`, `X-User-Roles` et `X-Operator-Id`.

### 5. Creation d'un OPERATOR_AGENT

`POST http://localhost:8080/api/auth/operator-agents`

Header : `Authorization: Bearer {{operatorAdminToken}}`

```json
{
  "firstName": "Agent",
  "lastName": "Express",
  "email": "agent@expressunion.cm",
  "password": "Password123@"
}
```

Le body ne contient ni role ni `operatorId`. Le backend impose `OPERATOR_AGENT` et recupere l'`operatorId` du compte `OPERATOR_ADMIN` authentifie. La liste des agents du meme operateur est disponible avec `GET /api/auth/operator-agents`.

### 6. Verification du profil

`GET http://localhost:8080/api/auth/me`

Header : `Authorization: Bearer {{operatorToken}}`

Le profil doit contenir le meme `operatorId` et le meme role que la reponse de login.

## Verification rapide

- [ ] L'organisation est creee avec le jeton `ADMIN_PLATFORM`.
- [ ] Le premier compte cree par la plateforme est uniquement `OPERATOR_ADMIN`.
- [ ] Un utilisateur operateur avec un `operatorId` inexistant est refuse.
- [ ] `OPERATOR_ADMIN` cree un agent sans envoyer d'`operatorId`.
- [ ] L'agent cree recoit le meme `operatorId` que son administrateur.
- [ ] `OPERATOR_AGENT` recoit 403 sur `/api/auth/operator-agents`.
- [ ] L'agent peut se connecter sur `/auth/operator`.
- [ ] Un client est refuse sur la page et les routes `/operator/**`.
- [ ] Le JWT contient `operatorId`.
- [ ] `/api/auth/me` retourne `operatorId`.
- [ ] Un agent ne voit pas les comptes d'un autre operateur.
- [ ] Une requete envoyant un faux `X-Operator-Id` ne contourne pas la gateway.
