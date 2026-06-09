# État & fonctionnement actuel de la plateforme

> Récapitulatif de ce qui est **construit et fonctionnel** à ce jour : à quoi sert
> chaque brique, comment ça marche, et comment les services communiquent.

## 1. Vue d'ensemble — ce qui tourne aujourd'hui

| Service | Port | Rôle | État |
|---------|------|------|------|
| **discovery-service** (Eureka) | 8761 | Annuaire : chaque service s'y enregistre et y trouve les autres | ✅ |
| **gateway-service** | 8080 | Porte d'entrée unique + **contrôle de sécurité JWT** | ✅ |
| **auth-service** | 8085 | Inscription, connexion, émission des jetons JWT | ✅ |
| **customer-service** | 8081 | Gestion des clients (CRUD) | ✅ (POST/GET) |
| account / transaction / loan | 8082-84 | (squelettes, pas encore de métier) | ⏳ |
| **postgres** | 5432 | Base de données (une par service) | ✅ |
| **rabbitmq** | 5672 / 15672 | Bus d'événements (pas encore utilisé) | ✅ (prêt) |

## 2. Comment une requête circule (le flux complet)

```
   [Client / Postman / Frontend]
            │  HTTP + JSON
            ▼
   ┌──────────────────────┐
   │   GATEWAY  (:8080)    │  ← SEUL point d'entrée public
   │  ┌────────────────┐   │
   │  │ Filtre JWT     │   │  1. /api/auth/** => public, laisse passer
   │  │ (videur)       │   │  2. sinon, exige un "Bearer <token>" valide
   │  └────────────────┘   │     - invalide/absent => 401 (rejet immédiat)
   │                       │     - valide => ajoute X-User-Email / X-User-Roles
   └──────────┬────────────┘
              │  routage via Eureka (lb://<service>)
   ┌──────────┼───────────────────────────────┐
   ▼          ▼                                ▼
auth-service  customer-service           (account, ...)
  (:8085)       (:8081)
   │              │
   ▼              ▼
 bank_auth_db   bank_customer_db        (PostgreSQL, 1 base/service)
```

**En clair** : tout passe par la Gateway sur `:8080`. Elle vérifie le jeton **une
seule fois**, puis route la requête vers le bon service grâce à Eureka.

## 3. Comment les services se trouvent : Eureka (Service Discovery)

On n'écrit **jamais** d'adresse IP en dur. Chaque service démarre et **s'enregistre**
dans Eureka avec son nom (`CUSTOMER-SERVICE`, `AUTH-SERVICE`…). La Gateway demande
ensuite à Eureka « où est `customer-service` ? » et obtient son adresse.

- C'est ce que signifie `uri: lb://customer-service` dans la config de la Gateway
  (`lb` = *load balanced*, via Eureka).
- Avantage : on peut lancer plusieurs instances d'un service, ou les déplacer, sans
  rien reconfigurer. Console : http://localhost:8761

## 4. Comment marche la sécurité (authentification JWT)

```
1. INSCRIPTION   POST /api/auth/register
                 -> auth-service hache le mot de passe (BCrypt) et cree l'utilisateur

2. CONNEXION     POST /api/auth/login  { email, motDePasse }
                 -> auth-service verifie, puis SIGNE un jeton JWT avec une cle secrete
                 -> renvoie { token, type: "Bearer", expiresIn }

3. APPELS        GET /api/customers/1   Header: Authorization: Bearer <token>
                 -> la GATEWAY verifie la signature du jeton (meme cle secrete)
                 -> si OK : route vers customer-service (+ en-tetes X-User-*)
                 -> si KO : 401, la requete n'atteint jamais le service
```

- **Pourquoi BCrypt ?** Le mot de passe n'est jamais stocké en clair ; même en cas de
  fuite de la base, il reste protégé.
- **Pourquoi JWT ?** Le jeton est *auto-porteur* et *signé* : le serveur n'a pas besoin
  de garder une session en mémoire (**stateless**), ce qui est idéal pour des
  microservices. Personne ne peut falsifier un jeton sans la clé secrète (`JWT_SECRET`).
- **Clé partagée** : `auth-service` **signe**, la Gateway **vérifie** — les deux
  utilisent le même `JWT_SECRET`.

## 5. Les 2 façons de communiquer

| Type | Quand | Comment | Utilisé ? |
|------|-------|---------|-----------|
| **Synchrone** (REST) | réponse immédiate attendue | HTTP via la Gateway / Eureka | ✅ oui |
| **Asynchrone** (événements) | notifier sans bloquer | RabbitMQ (`ClientCree`, `PretApprouve`…) | ⏳ prévu |

Aujourd'hui, tout est **synchrone**. Les événements RabbitMQ (voir
[contracts/02-evenements.md](contracts/02-evenements.md)) seront ajoutés quand les
services métier publieront des faits (ex : compte créé → notification).

## 6. Architecture interne d'un service (rappel)

Chaque service Java suit le même découpage en couches :
```
Controller  (HTTP/JSON)  ->  Service  (règles métier)  ->  Repository  (BDD)  ->  PostgreSQL
                 avec   Entity (objet en base)   et   DTO (objet en JSON)
```
Exemple concret et détaillé : `customer-service` (voir le code) et `auth-service`
(qui ajoute la couche `security/` : `JwtService`, `JwtAuthFilter`, `SecurityConfig`).

## 7. Lancer et tester

```bash
# Démarrer (depuis la racine du projet)
docker compose up -d

# Vérifier
docker compose ps
#  -> http://localhost:8761  (Eureka : services enregistrés)
```

**Tester les endpoints** : importer dans Postman le fichier
[`docs/postman/plateforme-bancaire.postman_collection.json`](postman/plateforme-bancaire.postman_collection.json),
puis exécuter dans l'ordre : **Register → Login** (le token est mémorisé
automatiquement) → **Me / Customers**.

> Rebuild après modif du code d'un service :
> `cd microservices-backend/<service> && ./mvnw -DskipTests package && cd ../.. && docker compose up -d --build <service>`

## 8. Ce qui reste

- Logique métier de account / transaction / loan
- Service OCR/IA (Python) + notifications (Node) + événements RabbitMQ
- Autorisation fine (les services lisent `X-User-Email` pour filtrer par utilisateur)
- Frontend, Kubernetes, CI/CD
- Voir le plan complet : [03-modele-domaine-et-plan.md](03-modele-domaine-et-plan.md)
