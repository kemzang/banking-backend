# Contrats REST (appels synchrones)

> Toutes les URL passent par la Gateway : `http://localhost:8080`.
> Voir les [conventions communes](README.md) (auth, erreurs, codes HTTP).
> ✏️ Ce sont des **propositions** : ajustez les champs en équipe, puis figez.

---

## 🔐 auth-service — `/api/auth`

| Méthode | URL | Rôle | Description |
|---------|-----|------|-------------|
| POST | `/api/auth/register` | public | Créer un compte utilisateur |
| POST | `/api/auth/login` | public | Se connecter, obtenir un JWT |
| GET | `/api/auth/me` | authentifié | Infos de l'utilisateur courant |

**POST /api/auth/register**
```json
// Requête
{ "email": "jean@mail.com", "motDePasse": "secret123", "telephone": "699000000" }
// Réponse 201
{ "id": "uuid-1", "email": "jean@mail.com", "roles": ["CLIENT"] }
```
**POST /api/auth/login**
```json
// Requête
{ "email": "jean@mail.com", "motDePasse": "secret123" }
// Réponse 200
{ "token": "eyJhbGciOi...", "type": "Bearer", "expiresIn": 86400 }
```

---

## 👤 customer-service — `/api/customers` et `/api/operators`

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/customers` | Inscrire un client |
| GET | `/api/customers/{id}` | Détails d'un client |
| GET | `/api/customers` | Lister (paginé) |
| PUT | `/api/customers/{id}` | Modifier un client |
| PATCH | `/api/customers/{id}/kyc` | Mettre à jour le statut KYC |
| GET | `/api/operators` | Lister les opérateurs |
| POST | `/api/operators` | Créer un opérateur (admin) |

**POST /api/customers**
```json
// Requête
{
  "utilisateurId": "uuid-1",
  "operateurId": 1,
  "nom": "Doe", "prenom": "Jean",
  "dateNaissance": "1995-04-12",
  "email": "jean@mail.com", "telephone": "699000000",
  "numeroIdentite": "1234567890", "typePiece": "CNI",
  "adresse": { "rue": "Rue 1", "ville": "Yaoundé", "pays": "Cameroun", "codePostal": "" }
}
// Réponse 201
{ "id": 5, "nom": "Doe", "prenom": "Jean", "statutKYC": "EN_ATTENTE", "operateurId": 1 }
```

**GET /api/customers/{id}** → `200`
```json
{ "id": 5, "nom": "Doe", "prenom": "Jean", "email": "jean@mail.com",
  "statutKYC": "VALIDE", "operateurId": 1 }
```

---

## 💳 account-service — `/api/accounts`

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/accounts` | Ouvrir un compte |
| GET | `/api/accounts/{id}` | Détails d'un compte |
| GET | `/api/accounts?clientId={id}` | Comptes d'un client |
| GET | `/api/accounts/{id}/balance` | Solde courant |
| POST | `/api/accounts/{id}/credit` | Créditer (usage interne transaction-service) |
| POST | `/api/accounts/{id}/debit` | Débiter (usage interne transaction-service) |

**POST /api/accounts**
```json
// Requête
{ "clientId": 5, "operateurId": 1, "type": "COURANT", "devise": "XAF" }
// Réponse 201
{ "id": 10, "numeroCompte": "CM-0001-0010", "clientId": 5, "solde": 0.00,
  "devise": "XAF", "statut": "ACTIF" }
```
> ⚠️ `credit`/`debit` renvoient `409 Conflict` si solde insuffisant ou plafond dépassé.

---

## 💸 transaction-service — `/api/transactions`

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/transactions/deposit` | Dépôt sur un compte |
| POST | `/api/transactions/withdraw` | Retrait |
| POST | `/api/transactions/transfer` | Transfert (intra/inter-opérateurs) |
| GET | `/api/transactions/{id}` | Détails |
| GET | `/api/transactions?accountId={id}` | Historique d'un compte |

**POST /api/transactions/transfer**
```json
// Requête
{ "compteSourceId": 10, "compteDestId": 22, "montant": 50000.00,
  "devise": "XAF", "motif": "Loyer" }
// Réponse 201
{ "id": 100, "reference": "TX-2026-000100", "type": "TRANSFERT",
  "montant": 50000.00, "commission": 250.00, "statut": "VALIDEE",
  "dateOperation": "2026-06-06T12:00:00Z" }
```

---

## 🏦 loan-service — `/api/loans`

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/loans/applications` | Soumettre une demande de prêt |
| GET | `/api/loans/applications/{id}` | Détails d'une demande |
| POST | `/api/loans/applications/{id}/decision` | Approuver/Rejeter (opérateur) |
| GET | `/api/loans/{id}` | Détails d'un prêt |
| GET | `/api/loans/{id}/schedule` | Échéancier du prêt |
| POST | `/api/loans/{id}/repay` | Rembourser une échéance |

**POST /api/loans/applications**
```json
// Requête
{ "clientId": 5, "montantDemande": 1000000.00, "dureeMois": 12, "motif": "Achat véhicule" }
// Réponse 201
{ "id": 30, "statut": "SOUMISE", "montantDemande": 1000000.00, "dateSoumission": "2026-06-06T12:00:00Z" }
```
**GET /api/loans/{id}/schedule** → `200`
```json
{ "pretId": 40, "echeances": [
  { "numero": 1, "dateEcheance": "2026-07-06", "montantTotal": 88849.00, "statut": "A_PAYER" },
  { "numero": 2, "dateEcheance": "2026-08-06", "montantTotal": 88849.00, "statut": "A_PAYER" }
] }
```

---

## 📄 ai-document-service — `/api/v1/...` (Python/FastAPI, port 8001)

> Implémentation réelle (Daryl). Service Python autonome (base SQLite), routé par
> la gateway via URL directe. Réponses enveloppées : `{ status, message, data }`.

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/v1/health/` | État du service |
| POST | `/api/v1/ocr/extract` | Extraire le texte d'une image (multipart : `file`) |
| GET | `/api/v1/ocr/history` | Historique des analyses OCR |
| GET | `/api/v1/ocr/history/{id}` | Détail d'une analyse OCR |
| GET | `/api/v1/analysis/...` | Analyse de documents |

**POST /api/v1/ocr/extract** (multipart : `file` = image PNG/JPG/JPEG) → `200`
```json
{
  "status": "success",
  "message": "OCR effectué avec succès",
  "data": { "id": 1, "texte_extrait": "...", "score_confiance": 0.94 }
}
```
> Codes d'erreur : `400` image invalide, `422` fichier absent, `503` Tesseract indisponible.
> Documentation interactive auto-générée : `http://localhost:8001/docs` (Swagger FastAPI).

---

## 🔔 notification-service — `/api/notifications`
Surtout piloté par **événements** (voir [02-evenements.md](02-evenements.md)), mais expose :

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/notifications?destinataireId={id}` | Notifications d'un utilisateur |
