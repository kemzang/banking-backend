# Flux multi-operateurs - recette Postman

Base URL : `http://localhost:8080`. Pour les routes protegees, ajouter
`Authorization: Bearer {{token}}`. Les identites `X-User-*` ne doivent jamais
etre envoyees par Postman : la gateway les reconstruit depuis le JWT.

## 1. Operateurs actifs

`GET /api/operators/active` (public) doit retourner uniquement `statut=ACTIVE`.

## 2. Inscription client

`POST /api/auth/register`

```json
{
  "firstName": "Jean",
  "lastName": "Dupont",
  "email": "jean.test@example.com",
  "password": "Password123@",
  "phone": "+237690000000",
  "operatorId": 5
}
```

Attendu : `200`, utilisateur `CLIENT`, `status=EN_ATTENTE`. Une tentative de
login avant validation retourne `423`. Une notification
`CLIENT_VALIDATION_REQUEST` est creee pour l'operateur 5.

## 3. Liste et notification operateur

Se connecter comme `OPERATOR_ADMIN`, puis :

- `GET /api/notifications` : uniquement les notifications de son operatorId ;
- `GET /api/customers/pending` : uniquement ses clients en attente.

## 4. Approbation / rejet client

- `PATCH /api/customers/{{clientId}}/approve`, body `{}` : statut KYC `VALIDE`
  et utilisateur auth `ACTIF` ; le client peut alors se connecter.
- `PATCH /api/customers/{{clientId}}/reject`, body
  `{"reason":"Document illisible"}` : statut `REJETE`, motif conserve et
  utilisateur auth `REJETE`.

Refaire ces appels avec un token `CLIENT`, `OPERATOR_AGENT` ou un
`OPERATOR_ADMIN` d'un autre operateur : attendu `403`.

## 5. Compte bancaire

`POST /api/accounts`

```json
{
  "clientId": 20,
  "operateurId": 999,
  "type": "COURANT",
  "devise": "XAF",
  "plafondJournalier": 500000,
  "decouvertAutorise": 0
}
```

Le backend ignore l'`operateurId` fourni et derive celui du profil client.
Attendu : `201`, `statut=EN_ATTENTE_ACTIVATION`, notification
`ACCOUNT_OPENING_REQUESTED`.

- `GET /api/accounts/pending`
- `PATCH /api/accounts/{{accountId}}/activate`
- `PATCH /api/accounts/{{accountId}}/reject`

Les decisions sont reservees a `OPERATOR_ADMIN` du meme operateur ou
`ADMIN_PLATFORM`.

## 6. Demande de pret

Avec un token `CLIENT`, `POST /api/loans` :

```json
{
  "accountId": 12,
  "montantDemande": 500000,
  "dureeMois": 12,
  "motif": "Equipement"
}
```

Attendu : clientId derive du JWT, compte obligatoirement `ACTIF`, operatorId
derive du compte/profil, statut `SOUMISE`, notification `LOAN_REQUESTED`.

- `GET /api/loans/my` : demandes du client connecte ;
- `GET /api/loans/pending` : demandes du seul operateur connecte ;
- `PATCH /api/loans/{{loanId}}/approve` avec
  `{"tauxInteret":0.12,"approuver":true,"compteId":null,"motifRejet":null}` ;
- `PATCH /api/loans/{{loanId}}/reject` avec `{"reason":"Risque trop eleve"}`.

Verifier qu'un `CLIENT`, un `OPERATOR_AGENT` et un autre operateur recoivent
`403` sur les decisions.

## 7. Notifications

- `GET /api/notifications` filtre par client ou operatorId ;
- `PATCH /api/notifications/{{notificationId}}/read`, body `{}` ;
- `POST /api/notifications` est reserve aux appels directs des microservices.

## Checklist

- [ ] Un operateur inactif n'apparait pas a l'inscription.
- [ ] Un compte client en attente ne peut pas se connecter.
- [ ] Un agent consulte mais ne valide ni client, ni compte, ni pret.
- [ ] Un operateur ne voit et ne traite que son perimetre.
- [ ] Les `clientId` et `operatorId` libres sont ignores ou controles.
- [ ] Les comptes en attente ne peuvent pas etre debites/credites.
- [ ] Un pret reference un compte actif appartenant au bon client.
- [ ] Les notifications sont filtrees et peuvent etre marquees lues.
